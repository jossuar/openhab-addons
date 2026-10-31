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

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoMessageFactory;
import org.openhab.binding.risco.internal.protocol.message.connection.Disconnect;
import org.openhab.binding.risco.internal.protocol.message.connection.Local;
import org.openhab.binding.risco.internal.protocol.message.connection.Remote;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A logged-in session with the panel over one {@link RiscoConnection}.
 *
 * The session numbers the commands it sends, matches the panel's responses to them, acknowledges the messages the
 * panel sends on its own and passes every received message to its listener. All this state belongs to one
 * connection, so a reconnection simply opens a new session.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
class RiscoSession implements RiscoConnection.Listener, Closeable {
    interface Listener {
        void messageReceived(RiscoMessage message);

        void sessionFailed(RiscoSession session, IOException cause);
    }

    // Like the Risco software, a session starts with command id 2. Ids 50 to 99 are used by the panel.
    private static final int FIRST_COMMAND_ID = 2;
    private static final int LAST_COMMAND_ID = 45;
    private static final long RESPONSE_TIMEOUT_MS = 15000;
    private static final long DISCONNECT_TIMEOUT_MS = 1000;
    private static final Pattern ERROR_RESPONSE = Pattern.compile("N\\d+");
    private static final Pattern SECRET_COMMAND = Pattern.compile("(RMT|UPIN\\d*|INSTPIN|SUBPIN)=.*");

    private final Logger logger = LoggerFactory.getLogger(RiscoSession.class);

    private final RiscoConnectionSettings settings;
    private final RiscoConnection connection;
    private final Listener listener;
    private final RiscoMessageFactory messageFactory = new RiscoMessageFactory();
    private final Map<Integer, CompletableFuture<RiscoMessage>> pendingResponses = new ConcurrentHashMap<>();

    private final Object sendLock = new Object();
    private int nextCommandId = FIRST_COMMAND_ID;

    private volatile long lastReceiveTime = System.nanoTime();

    private RiscoSession(RiscoConnectionSettings settings, RiscoConnection connection, Listener listener) {
        this.settings = settings;
        this.connection = connection;
        this.listener = listener;
    }

    /**
     * Connects to the panel and logs in.
     *
     * @throws IOException if the panel cannot be reached or rejects the login
     */
    static RiscoSession open(RiscoConnectionSettings settings, Listener listener)
            throws IOException, InterruptedException {
        RiscoConnection connection = RiscoConnection.open(settings.hostname(), settings.port());
        RiscoSession session = new RiscoSession(settings, connection, listener);
        try {
            connection.startReading("OH-binding-" + settings.name() + "-risco-reader", session);
            session.logIn();
            return session;
        } catch (IOException | InterruptedException | RuntimeException e) {
            session.close();
            throw e;
        }
    }

    /**
     * Sends an encrypted command without waiting for the response.
     */
    void send(String command) throws IOException {
        transmit(command, true, null);
    }

    boolean isOpen() {
        return connection.isOpen();
    }

    long millisSinceLastReceive() {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - lastReceiveTime);
    }

    /**
     * Tells the panel that the session ends, then closes the connection.
     */
    void disconnect() {
        try {
            sendAndWait(Disconnect.getReadCommand(), true, DISCONNECT_TIMEOUT_MS);
        } catch (IOException e) {
            logger.debug("The panel did not acknowledge the disconnection: {}", e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        close();
    }

    @Override
    public void close() {
        connection.close();
        failPendingResponses(new IOException("The session was closed"));
    }

    @Override
    public void frameReceived(byte[] frame) {
        RiscoMessage message;
        try {
            message = messageFactory.create(settings.panelId(), settings.encoding(), frame);
        } catch (RuntimeException e) {
            logger.debug("Discarding an invalid frame {}: {}", HexUtils.bytesToHex(frame), e.getMessage());
            return;
        }

        lastReceiveTime = System.nanoTime();
        logger.debug("<---- {}", describe(message));

        switch (message.getMessageOrigin()) {
            case PANEL -> acknowledge(message);
            case BINDING -> completePendingResponse(message);
            default -> {
            }
        }

        listener.messageReceived(message);
    }

    @Override
    public void connectionFailed(IOException cause) {
        failPendingResponses(cause);
        listener.sessionFailed(this, cause);
    }

    private void logIn() throws IOException, InterruptedException {
        if (settings.connectionDelayMs() > 0) {
            // Older panels need some time after the connection is opened
            Thread.sleep(settings.connectionDelayMs());
        }

        // The login commands are sent unencrypted
        checkLoginResponse(sendAndWait(Remote.getReadCommand(settings.password()), false, RESPONSE_TIMEOUT_MS));
        checkLoginResponse(sendAndWait(Local.getReadCommand(), false, RESPONSE_TIMEOUT_MS));
    }

    private static void checkLoginResponse(RiscoMessage response) throws IOException {
        if (ERROR_RESPONSE.matcher(response.getCommandName()).matches()) {
            throw new IOException("The panel rejected the login with error " + response.getCommandName()
                    + ". Check the panel ID and the password.");
        }
    }

    private RiscoMessage sendAndWait(String command, boolean encrypt, long timeoutMs)
            throws IOException, InterruptedException {
        CompletableFuture<RiscoMessage> response = new CompletableFuture<>();
        int commandId = transmit(command, encrypt, response);
        try {
            return response.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new IOException("The panel did not respond to command " + commandId);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            throw cause instanceof IOException ioException ? ioException : new IOException(cause);
        } finally {
            pendingResponses.remove(commandId, response);
        }
    }

    /**
     * Sends a command with the next command id.
     *
     * @param response completed with the panel's response, if not null
     * @return the command id used
     */
    private int transmit(String command, boolean encrypt, @Nullable CompletableFuture<RiscoMessage> response)
            throws IOException {
        // Ids are given and written under one lock, so the panel receives them in order
        synchronized (sendLock) {
            int commandId = nextCommandId;
            nextCommandId = commandId == LAST_COMMAND_ID ? 1 : commandId + 1;

            if (response != null) {
                pendingResponses.put(commandId, response);
            }
            try {
                write(messageFactory.create(settings.panelId(), settings.encoding(), commandId, command, encrypt));
            } catch (IOException e) {
                pendingResponses.remove(commandId);
                throw e;
            }
            return commandId;
        }
    }

    /**
     * Acknowledges a message sent by the panel on its own. The panel repeats unacknowledged messages, so the
     * acknowledgement is written right away and never waits for anything.
     */
    private void acknowledge(RiscoMessage message) {
        try {
            write(messageFactory.create(settings.panelId(), settings.encoding(), message.getCommandId(), "ACK",
                    true));
        } catch (IOException e) {
            logger.debug("Could not acknowledge message {}: {}", message.getCommandId(), e.getMessage());
        }
    }

    private void write(RiscoMessage message) throws IOException {
        logger.debug("----> {}", describe(message));
        connection.write(message.getEncryptedMessage());
    }

    private void completePendingResponse(RiscoMessage message) {
        CompletableFuture<RiscoMessage> response = pendingResponses.remove(message.getCommandId());
        if (response != null) {
            response.complete(message);
        }
    }

    private void failPendingResponses(IOException cause) {
        pendingResponses.values().forEach(response -> response.completeExceptionally(cause));
        pendingResponses.clear();
    }

    /**
     * Describes a message for the logs, hiding passwords and user codes.
     */
    private static String describe(RiscoMessage message) {
        String command = message.getFullCommand();
        if (SECRET_COMMAND.matcher(command).matches()) {
            command = command.substring(0, command.indexOf('=') + 1) + "****";
        }
        return message.getCommandId() + " " + command;
    }
}
