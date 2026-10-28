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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.protocol.MessageOrigin;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoMessageFactory;
import org.openhab.binding.risco.internal.protocol.message.connection.Disconnect;
import org.openhab.binding.risco.internal.protocol.message.connection.Local;
import org.openhab.binding.risco.internal.protocol.message.connection.Remote;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoCommunicator} is responsible for the asynchronous serial communication
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoCommunicator {
    private final Logger logger = LoggerFactory.getLogger(RiscoCommunicator.class);

    private final String uid;
    private final String hostname;
    private final int port;
    private final int panelId;
    private final String encoding;
    private final String password;
    private final int connectionDelayMs;

    // The connection state is changed by the watchdog thread and used by the sender and receiver threads
    private volatile Socket tcpSocket;
    private volatile BufferedOutputStream tcpOutput;
    private volatile BufferedInputStream tcpInput;
    private volatile boolean connected = false;

    // start, stop and reconnect must not run at the same time
    private final Object lifecycleLock = new Object();
    private volatile boolean stopped = false;

    // send
    private volatile Thread riscoSender;
    private final LinkedBlockingDeque<RiscoMessage> sendQueue = new LinkedBlockingDeque<RiscoMessage>(50);
    private int sendCommandId = 2;

    // receive
    private volatile Thread riscoReceiver;

    // listener
    private final Set<RiscoPanelListener> listenerQueue = new CopyOnWriteArraySet<>();

    // watchdog
    private volatile ZonedDateTime lastSendTime = ZonedDateTime.now();
    private volatile ZonedDateTime lastReceiveTime = ZonedDateTime.now();
    private ScheduledFuture<?> riscoWatchdog;
    private volatile boolean connectionUp = true;

    public interface RiscoPanelListener {
        public void handleRiscoMessage(RiscoMessage msg);

        default void connectionStateChanged(boolean up) {
        }
    }

    public void addListener(RiscoPanelListener listener) {
        listenerQueue.add(listener);
    }

    public RiscoCommunicator(String uid, String hostname, int port, int panelId, String encoding, String password,
            ScheduledExecutorService scheduler) throws IOException {
        this(uid, hostname, port, panelId, encoding, password, 0, scheduler);
    }

    public RiscoCommunicator(String uid, String hostname, int port, int panelId, String encoding, String password,
            int connectionDelayMs, ScheduledExecutorService scheduler) throws IOException {
        logger.trace("RiscoCommunicator(): Connecting to Risco panel");

        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
        this.panelId = panelId;
        this.encoding = encoding;
        this.password = password;
        this.connectionDelayMs = Math.max(0, connectionDelayMs);

        // Open the socket and get the streams
        Socket socket = connectSocket(hostname, port);
        try {
            tcpOutput = new BufferedOutputStream(socket.getOutputStream());
            tcpInput = new BufferedInputStream(socket.getInputStream());
        } catch (IOException e) {
            closeQuietly(socket);
            throw e;
        }
        tcpSocket = socket;

        // Start sender, receiver and watchdog threads
        riscoReceiver = new Thread(new RiscoReceiver(), "OH-binding-" + uid + "-riscorecvr");
        riscoReceiver.setDaemon(true);
        riscoReceiver.start();

        riscoSender = new Thread(new RiscoSender(), "OH-binding-" + uid + "-riscosender");
        riscoSender.setDaemon(true);
        riscoSender.start();

        riscoWatchdog = scheduler.scheduleWithFixedDelay(new RiscoWatchdog(), 10, 20, TimeUnit.SECONDS);

        connected = true;
        logger.trace("RiscoCommunicator communication threads started successfully");
    }

    /**
     * Opens a connection to the panel. The socket is closed again if the connection cannot be established.
     */
    private static Socket connectSocket(String hostname, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(hostname, port), 5000);
        } catch (IOException | RuntimeException e) {
            closeQuietly(socket);
            throw e;
        }
        return socket;
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Nothing else can be done
        }
    }

    private void start() throws IOException {
        logger.trace("start(): RiscoCommunicator starting");

        if (stopped) {
            throw new IOException("The communicator was stopped");
        }

        // Reset command id
        synchronized (this) {
            sendCommandId = 2;
        }

        // Drop anything left in the queue by a previous connection, e.g. a disconnect command that could not be sent
        sendQueue.clear();

        // Open the socket and get the streams
        Socket socket = connectSocket(hostname, port);
        try {
            tcpOutput = new BufferedOutputStream(socket.getOutputStream());
            tcpInput = new BufferedInputStream(socket.getInputStream());
        } catch (IOException e) {
            closeQuietly(socket);
            throw e;
        }
        tcpSocket = socket;

        // Start sender and receiver threads
        riscoReceiver = new Thread(new RiscoReceiver(), "OH-binding-" + uid + "-riscorecvr");
        riscoReceiver.setDaemon(true);
        riscoReceiver.start();

        riscoSender = new Thread(new RiscoSender(), "OH-binding-" + uid + "-riscosender");
        riscoSender.setDaemon(true);
        riscoSender.start();

        // Initialize the communication with the panel
        try {
            initializeSession();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while initializing the session");
        }

        connected = true;
    }

    /**
     * Initializes the communication with the panel. This must be done once the connection is open, after the
     * configured connection delay.
     */
    public void initializeSession() throws InterruptedException {
        if (connectionDelayMs > 0) {
            Thread.sleep(connectionDelayMs);
        }

        // Stop if the thread is being interrupted, e.g. because the communicator is being stopped
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        sendPlainAndWait(Remote.getReadCommand(password));

        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        sendPlainAndWait(Local.getReadCommand());
    }

    private void stopInternal() {
        logger.trace("stopInternal(): RiscoCommunicator stopping");
        connected = false;

        synchronized (this) {
            if (responseCommandId != null) {
                logger.debug("Clearing pending response wait for command id {} due to disconnect.", responseCommandId);
                responseCommandId = null;
                notifyAll();
            }
        }

        // Disconnect command. Without a running sender it would just stay in the queue.
        if (riscoSender.isAlive()) {
            try {
                send(Disconnect.getReadCommand());
            } catch (Exception exception) {
                logger.debug("stopInternal(): Error sending the disconnect command - {}", exception.getMessage());
            }
        }

        // Wait a second to receive the ACK from the panel before closing the socket
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        // Interrupt threads
        riscoReceiver.interrupt();
        riscoSender.interrupt();

        // Close streams
        try {
            tcpInput.close();
        } catch (IOException e) {
        }
        try {
            tcpOutput.close();
        } catch (IOException e) {
        }

        // Close socket
        try {
            tcpSocket.close();
            logger.trace("stopInternal(): Closed TCP Connection!");
        } catch (IOException ioException) {
            logger.debug("stopInternal(): Unable to close connection - {}", ioException.getMessage());
        } catch (Exception exception) {
            logger.debug("stopInternal(): Error closing connection - {}", exception.getMessage());
        }

        // Wait until communication threads exit
        try {
            riscoReceiver.join(3000);
        } catch (InterruptedException e) {
        }
        try {
            riscoSender.join(3000);
        } catch (InterruptedException e) {
        }

        // Nothing must be sent on the next connection from this one
        sendQueue.clear();
    }

    private void notifyConnectionState(boolean up) {
        if (connectionUp == up) {
            return;
        }
        connectionUp = up;

        for (RiscoPanelListener listener : listenerQueue) {
            try {
                listener.connectionStateChanged(up);
            } catch (RuntimeException e) {
                logger.debug("Listener failed on connection state change: {}", e.getMessage());
            }
        }
    }

    public void stop() {
        logger.trace("stop(): RiscoCommunicator stopping");

        // Stop the watchdog first (interrupting a reconnection in progress), so it cannot open a new connection
        // after the communicator was stopped
        stopped = true;
        riscoWatchdog.cancel(true);

        synchronized (lifecycleLock) {
            stopInternal();
        }
    }

    private static final long RESPONSE_TIMEOUT_MS = 15000;

    @Nullable
    private Integer responseCommandId;

    public synchronized void suspendForPendingResponseIfNeeded() {
        long deadlineMillis = System.currentTimeMillis() + RESPONSE_TIMEOUT_MS;

        while (responseCommandId != null) {
            long remainingMillis = deadlineMillis - System.currentTimeMillis();
            if (remainingMillis <= 0) {
                responseCommandId = null;
                break;
            }
            try {
                wait(remainingMillis);
            } catch (InterruptedException e) {
                // Restore the flag and stop waiting. Looping here would spin, as wait() throws again immediately.
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private synchronized void send(String command, boolean encrypt, boolean wait) {
        suspendForPendingResponseIfNeeded();

        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg = factory.create(panelId, encoding, sendCommandId, command, encrypt);

        if (!enqueue(msg)) {
            return;
        }

        if (wait) {
            responseCommandId = sendCommandId;
        }

        // adjust command id for the next send
        sendCommandId++;
        if (sendCommandId == 46) {
            sendCommandId = 1;
        }
    }

    public synchronized void sendPlainAndWait(String command) {
        send(command, false, true);
    }

    public synchronized void sendAndWait(String command) {
        send(command, true, true);
    }

    public synchronized void sendPlain(String command) {
        send(command, false, false);
    }

    public synchronized void send(String command) {
        send(command, true, false);
    }

    public synchronized void sendFirst(int commandId, String command) {
        suspendForPendingResponseIfNeeded();

        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg = factory.create(panelId, encoding, commandId, command, true);

        enqueue(msg);
    }

    private boolean enqueue(RiscoMessage msg) {
        if (!sendQueue.offer(msg)) {
            logger.debug("Send queue is full, dropping message {}", msg);
            return false;
        }
        return true;
    }

    private synchronized void handleIncomingMessage(RiscoMessage msg) {
        logger.debug("<---- {}", msg);

        lastReceiveTime = ZonedDateTime.now();

        if (responseCommandId != null && msg.getCommandId().equals(responseCommandId)) {
            responseCommandId = null;
            notifyAll();
        }

        if (msg.getMessageOrigin() == MessageOrigin.PANEL) {
            sendFirst(msg.getCommandId(), "ACK");
        }

        for (RiscoPanelListener listener : listenerQueue) {
            listener.handleRiscoMessage(msg);
        }
    }

    private void handleOutgoingMessage(RiscoMessage msg) {
        logger.debug("----> {}", msg);

        lastSendTime = ZonedDateTime.now();

        for (RiscoPanelListener listener : listenerQueue) {
            listener.handleRiscoMessage(msg);
        }
    }

    public Boolean isConnected() {
        return connected;
    }

    private class RiscoReceiver implements Runnable {
        private static final int MAX_MESSAGE_SIZE = 4096;
        private byte[] buffer = new byte[MAX_MESSAGE_SIZE];
        private int bufferIndex = 0;
        private boolean unStuff = false;
        private boolean stuffed = false;

        /**
         * Run method. Runs the MessageListener thread
         */
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                readMessageBuffer();
            }

            logger.trace("RiscoReceiver. Thread stopped.");
        }

        private void readMessageBuffer() {
            try {
                InputStream is = tcpInput;

                // Initialize
                bufferIndex = 0;
                unStuff = false;
                stuffed = false;

                // Read start byte 0x02
                loopUntilByteIsRead(is, 0x02);
                buffer[bufferIndex] = 0x02;
                bufferIndex++;

                // Read until end byte 0x03
                do {
                    int b = readUnstuffedByte(is);

                    if (bufferIndex >= MAX_MESSAGE_SIZE) {
                        logger.debug("Message exceeded {} bytes, discarding it.", MAX_MESSAGE_SIZE);
                        return;
                    }

                    buffer[bufferIndex] = (byte) b;
                    bufferIndex++;

                    if (b == 0x03 && !stuffed) {
                        break;
                    }
                } while (true);

                byte[] message = Arrays.copyOfRange(buffer, 0, bufferIndex);

                logger.trace("<---- {}", HexUtils.bytesToHex(message));

                RiscoMessageFactory factory = new RiscoMessageFactory();
                RiscoMessage msg = factory.create(panelId, encoding, message);
                handleIncomingMessage(msg);

                logger.trace("RiscoCommunicator.read() Got message");
            } catch (EOFException e) {
                logger.debug("RiscoReceiver: Connection closed by the panel.");
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                logger.debug("RiscoReceiver: IO Exception.");
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                logger.debug("RiscoReceiver: Error processing a received message, discarding it. {}", e.getMessage());
            }
        }

        private void loopUntilByteIsRead(InputStream stream, int byteToRead) throws IOException {
            int b = 0;
            do {
                b = readUnstuffedByte(stream);
            } while (b != byteToRead);
        }

        private int readUnstuffedByte(InputStream stream) throws IOException {
            int b;

            if (!unStuff) {
                stuffed = false;
                b = readByte(stream);

                if (b == 0x10) {
                    unStuff = true;
                } else {
                    return b;
                }
            }

            b = readByte(stream);
            stuffed = true;
            unStuff = false;

            return b;
        }

        private int readByte(InputStream stream) throws IOException {
            int b = stream.read();
            if (b == -1) {
                throw new EOFException();
            }
            return b;
        }
    }

    private class RiscoSender implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RiscoMessage rm = sendQueue.take();
                    write(rm.getEncryptedMessage());

                    handleOutgoingMessage(rm);
                } catch (InterruptedException e) {
                    // Just exit the loop
                    logger.debug("RiscoCommunicator.SenderThread: InterruptedException caught.");
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    // The message is lost, but keep draining the queue. If the connection is really broken, the
                    // watchdog will notice that nothing is being sent and will reconnect.
                    logger.debug("RiscoCommunicator.SenderThread: IOException caught: {}", e.getMessage());
                } catch (RuntimeException e) {
                    logger.debug("RiscoCommunicator.SenderThread: Unexpected error: {}", e.getMessage());
                }
            }

            logger.trace("RiscoSender. Thread stopped.");
        }

        private void write(byte[] buffer) throws IOException {
            tcpOutput.write(buffer);
            tcpOutput.flush();
            logger.trace("write(): Message Sent: {}", buffer);
        }
    }

    private void reconnect() throws Exception {
        synchronized (lifecycleLock) {
            if (stopped) {
                throw new IOException("The communicator was stopped");
            }

            stopInternal();
            start();
        }
    }

    private class RiscoWatchdog implements Runnable {
        @Override
        public void run() {
            if (stopped) {
                return;
            }

            if (ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()) > 30
                    || ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()) > 70) {
                logger.trace("check sendBefore: {}, recvBefore: {}, result: {}",
                        ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()),
                        ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()), "--");

                logger.trace("Reconnecting");
                try {
                    lastSendTime = ZonedDateTime.now();
                    lastReceiveTime = ZonedDateTime.now();
                    notifyConnectionState(false);
                    reconnect();
                    notifyConnectionState(true);
                } catch (Exception e) {
                    logger.debug("Could not reconnect to the panel: {}", e.getMessage());
                }
                return;
            } else {
                logger.trace("check sendBefore: {}, recvBefore: {}, result: {}",
                        ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()),
                        ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()), "OK");
            }

            try {
                send("CLOCK");
            } catch (RuntimeException e) {
                logger.debug("Could not send the keepalive to the panel: {}", e.getMessage());
            }
        }
    }
}
