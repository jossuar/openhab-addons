/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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

    private Socket tcpSocket;
    private BufferedOutputStream tcpOutput;
    private BufferedInputStream tcpInput;
    private Boolean connected = false;

    // send
    private Thread riscoSender;
    private final LinkedBlockingDeque<RiscoMessage> sendQueue = new LinkedBlockingDeque<RiscoMessage>(50);
    private int sendCommandId = 2;

    // receive
    private Thread riscoReceiver;

    // in-flight
    // private final BlockingDeque<RiscoMessagePair> inFlightQueue = new LinkedBlockingDeque<RiscoMessagePair>();

    // listener
    private final Set<RiscoPanelListener> listenerQueue = new HashSet<>();

    // watchdog
    private ZonedDateTime lastSendTime = ZonedDateTime.now();
    private ZonedDateTime lastReceiveTime = ZonedDateTime.now();
    private ScheduledFuture<?> riscoWatchdog;

    public interface RiscoPanelListener {
        public void handleRiscoMessage(RiscoMessage msg);
    }

    public void addListener(RiscoPanelListener listener) {
        listenerQueue.add(listener);
    }

    public RiscoCommunicator(String uid, String hostname, int port, int panelId, String encoding, String password,
            ScheduledExecutorService scheduler) throws IOException {
        logger.trace("RiscoCommunicator(): Connecting to Risco panel");

        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
        this.panelId = panelId;
        this.encoding = encoding;
        this.password = password;

        // Open the socket and get the streams
        tcpSocket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(hostname, port);
        tcpSocket.connect(socketAddress, 5000);
        tcpOutput = new BufferedOutputStream(tcpSocket.getOutputStream());
        tcpInput = new BufferedInputStream(tcpSocket.getInputStream());

        // Start sender, receiver and watchdog threads
        riscoReceiver = new Thread(new RiscoReceiver(), "OH-binding-" + uid + "-riscorecevr");
        riscoReceiver.setDaemon(true);
        riscoReceiver.start();

        riscoSender = new Thread(new RiscoSender(), "OH-binding-" + uid + "-riscosender");
        riscoSender.setDaemon(true);
        riscoSender.start();

        riscoWatchdog = scheduler.scheduleWithFixedDelay(new RiscoWatchdog(), 10, 20, TimeUnit.SECONDS);

        connected = true;
        logger.trace("RiscoCommunicator communication threads started successfully");
    }

    private void start() throws IOException {
        logger.trace("start(): RiscoCommunicator starting");
        // Reset command id
        sendCommandId = 2;

        // Open the socket and get the streams
        tcpSocket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(hostname, port);
        tcpSocket.connect(socketAddress, 5000);
        tcpOutput = new BufferedOutputStream(tcpSocket.getOutputStream());
        tcpInput = new BufferedInputStream(tcpSocket.getInputStream());

        // Start sender and receiver threads
        riscoReceiver = new Thread(new RiscoReceiver(), "OH-binding-" + uid + "-riscorecvr");
        riscoReceiver.setDaemon(true);
        riscoReceiver.start();

        riscoSender = new Thread(new RiscoSender(), "OH-binding-" + uid + "-riscosender");
        riscoSender.setDaemon(true);
        riscoSender.start();

        // Initialize the communication with the panel
        sendPlainAndWait(Remote.getReadCommand(password));
        sendPlainAndWait(Local.getReadCommand());

        connected = true;
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

        // Disconnect command
        try {
            send(Disconnect.getReadCommand());
        } catch (Exception exception) {
            logger.debug("send(): Error closing connection - {}", exception.getMessage());
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
            logger.trace("closeConnection(): Closed TCP Connection!");
        } catch (IOException ioException) {
            logger.debug("closeConnection(): Unable to close connection - {}", ioException.getMessage());
        } catch (Exception exception) {
            logger.debug("closeConnection(): Error closing connection - {}", exception.getMessage());
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
    }

    public void stop() {
        logger.trace("stop(): RiscoCommunicator stopping");
        stopInternal();

        // Stop watchdog
        riscoWatchdog.cancel(true);
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
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void send(String command, boolean encrypt, boolean wait) {
        suspendForPendingResponseIfNeeded();

        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg = factory.create(panelId, encoding, sendCommandId, command, encrypt);

        if (wait) {
            responseCommandId = sendCommandId;
        }
        sendQueue.add(msg);

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

        sendQueue.add(msg);
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

                    buffer[bufferIndex] = (byte) b;
                    bufferIndex++;
                    if (bufferIndex == MAX_MESSAGE_SIZE) {
                        logger.debug("Message exceeded {} bytes.", MAX_MESSAGE_SIZE);

                        // Stop the receiver
                        Thread.currentThread().interrupt();
                    }

                    if (b == 0x03 && !stuffed) {
                        break;
                    }
                } while (true);

                byte[] message = Arrays.copyOfRange(buffer, 0, bufferIndex);

                logger.trace("<---- {}", HexUtils.bytesToHex(message));

                RiscoMessageFactory factory = new RiscoMessageFactory();
                RiscoMessage msg = factory.create(1, "UTF-8", message);
                handleIncomingMessage(msg);

                logger.trace("RiscoCommunicator.read() Got message");
            } catch (EOFException e) {
                return;
            } catch (IOException e) {
                Thread.currentThread().interrupt();
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
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    RiscoMessage rm = sendQueue.take();
                    write(rm.getEncryptedMessage());

                    handleOutgoingMessage(rm);
                }

                logger.trace("RiscoCommunicator.SenderThread: Thread interrupted.");
            } catch (InterruptedException e) {
                // Just exit the loop
                logger.debug("RiscoCommunicator.SenderThread: InterruptedException caught.");
            } catch (IOException e) {
                logger.debug("RiscoCommunicator.SenderThread: IOException caught.", e);
            }

            logger.trace("RiscoSender. Thread stopped.");
        }

        private void write(byte[] buffer) throws IOException {
            tcpOutput.write(buffer);
            tcpOutput.flush();
            logger.trace("write(): Message Sent: {}", buffer);
        }
    }

    private void reconnect() throws IOException {
        stopInternal();
        start();
    }

    private class RiscoWatchdog implements Runnable {
        @Override
        public void run() {
            if (ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()) > 30
                    || ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()) > 70) {
                logger.trace("check sendBefore: {}, recvBefore: {}, result: {}",
                        ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()),
                        ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()), "--");

                logger.trace("Reconnecting");
                try {
                    lastSendTime = ZonedDateTime.now();
                    lastReceiveTime = ZonedDateTime.now();
                    reconnect();
                } catch (IOException e) {
                    logger.debug("Could not reconnect to the panel.", e);
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
            }
        }
    }
}
