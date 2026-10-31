/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.risco.internal;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoCommunicator} keeps the binding connected to the panel.
 *
 * It opens a {@link RiscoSession}, passes the received messages to the listeners, sends a keepalive and opens a new
 * session when the connection is lost.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoCommunicator implements RiscoSession.Listener {
    private static final long WATCHDOG_INTERVAL_S = 20;
    private static final long RECONNECT_DELAY_S = 5;
    private static final long MAX_SILENCE_MS = 70000;
    private static final String KEEPALIVE_COMMAND = "CLOCK";

    private final Logger logger = LoggerFactory.getLogger(RiscoCommunicator.class);

    private final RiscoConnectionSettings settings;
    private final ScheduledExecutorService scheduler;
    private final Set<RiscoPanelListener> listeners = new CopyOnWriteArraySet<>();

    // Serializes opening and closing sessions
    private final Object lifecycleLock = new Object();
    private final AtomicReference<@Nullable RiscoSession> session = new AtomicReference<>();
    private final AtomicBoolean connectionUp = new AtomicBoolean(false);
    private volatile boolean stopped = false;
    private volatile @Nullable ScheduledFuture<?> watchdogJob;
    private volatile @Nullable ScheduledFuture<?> reconnectJob;

    public RiscoCommunicator(RiscoConnectionSettings settings, ScheduledExecutorService scheduler) {
        this.settings = settings;
        this.scheduler = scheduler;
    }

    public void addListener(RiscoPanelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(RiscoPanelListener listener) {
        listeners.remove(listener);
    }

    /**
     * Connects and logs in to the panel. Afterwards, the connection is kept up until {@link #stop()} is called.
     *
     * @throws IOException if the panel cannot be reached or rejects the login
     */
    public void start() throws IOException, InterruptedException {
        synchronized (lifecycleLock) {
            if (stopped) {
                throw new IllegalStateException("The communicator was stopped");
            }
            session.set(RiscoSession.open(settings, this));
            connectionUp.set(true);
            watchdogJob = scheduler.scheduleWithFixedDelay(this::watchdog, WATCHDOG_INTERVAL_S, WATCHDOG_INTERVAL_S,
                    TimeUnit.SECONDS);
        }
    }

    /**
     * Disconnects from the panel. A stopped communicator cannot be started again.
     */
    public void stop() {
        stopped = true;
        // Interrupt a reconnection in progress, so the lock below is released quickly
        cancel(watchdogJob);
        cancel(reconnectJob);

        synchronized (lifecycleLock) {
            RiscoSession current = session.getAndSet(null);
            if (current != null) {
                current.disconnect();
            }
        }
    }

    /**
     * Sends an encrypted command to the panel.
     *
     * @return false if the command could not be sent, e.g. because the panel is not connected
     */
    public boolean send(String command) {
        RiscoSession current = session.get();
        if (current == null) {
            logger.debug("Not connected to the panel, command dropped: {}", command);
            return false;
        }

        try {
            current.send(command);
            return true;
        } catch (IOException e) {
            logger.debug("Could not send command {}: {}", command, e.getMessage());
            return false;
        }
    }

    public boolean isConnected() {
        return session.get() != null;
    }

    @Override
    public void messageReceived(RiscoMessage message) {
        for (RiscoPanelListener listener : listeners) {
            try {
                listener.handleRiscoMessage(message);
            } catch (RuntimeException e) {
                logger.debug("Listener failed to handle message {}: {}", message.getCommandName(), e.getMessage(), e);
            }
        }
    }

    @Override
    public void sessionFailed(RiscoSession failedSession, IOException cause) {
        if (connectionLost(failedSession, cause.getMessage()) && !stopped) {
            reconnectJob = scheduler.schedule(this::reconnect, RECONNECT_DELAY_S, TimeUnit.SECONDS);
        }
    }

    /**
     * Runs periodically: sends the keepalive, checks that the panel still answers and retries failed reconnections.
     */
    private void watchdog() {
        RiscoSession current = session.get();
        if (current == null) {
            reconnect();
            return;
        }

        if (!current.isOpen() || current.millisSinceLastReceive() > MAX_SILENCE_MS) {
            connectionLost(current, "no data received for " + current.millisSinceLastReceive() / 1000 + " s");
            reconnect();
            return;
        }

        send(KEEPALIVE_COMMAND);
    }

    /**
     * Drops a session that stopped working.
     *
     * @return true if the session was the current one, false if it was already dropped
     */
    private boolean connectionLost(RiscoSession lostSession, @Nullable String reason) {
        if (!session.compareAndSet(lostSession, null)) {
            return false;
        }
        logger.debug("Connection to the panel lost: {}", reason);
        lostSession.close();
        notifyConnectionState(false);
        return true;
    }

    private void reconnect() {
        synchronized (lifecycleLock) {
            if (stopped || session.get() != null) {
                return;
            }
            try {
                session.set(RiscoSession.open(settings, this));
                logger.debug("Reconnected to the panel");
                notifyConnectionState(true);
            } catch (IOException e) {
                logger.debug("Could not reconnect to the panel, retrying in {} s: {}", WATCHDOG_INTERVAL_S,
                        e.getMessage());
            } catch (InterruptedException e) {
                // The communicator is being stopped
                Thread.currentThread().interrupt();
            }
        }
    }

    private void notifyConnectionState(boolean up) {
        if (connectionUp.getAndSet(up) == up) {
            return;
        }
        for (RiscoPanelListener listener : listeners) {
            try {
                listener.connectionStateChanged(up);
            } catch (RuntimeException e) {
                logger.debug("Listener failed on connection state change: {}", e.getMessage(), e);
            }
        }
    }

    private static void cancel(@Nullable ScheduledFuture<?> job) {
        if (job != null) {
            job.cancel(true);
        }
    }
}
