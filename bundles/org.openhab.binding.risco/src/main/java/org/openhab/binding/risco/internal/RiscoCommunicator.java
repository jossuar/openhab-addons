/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import org.openhab.binding.risco.internal.message.MessageOrigin;
import org.openhab.binding.risco.internal.message.RiscoMessage;
import org.openhab.binding.risco.internal.message.RiscoMessageFactory;
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

    private ScheduledExecutorService scheduler;

    public interface RiscoPanelListener {
        public void handleRiscoMessage(RiscoMessage msg);
    }

    public void addListener(RiscoPanelListener listener) {
        listenerQueue.add(listener);
    }

    public RiscoCommunicator(String uid, String hostname, int port, int panelId, String encoding, String password,
            ScheduledExecutorService scheduler) throws IOException {
        logger.debug("RiscoCommunicator(): Connecting to Risco panel");

        this.uid = uid;
        this.hostname = hostname;
        this.port = port;
        this.panelId = panelId;
        this.encoding = encoding;
        this.password = password;
        this.scheduler = scheduler;

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

        // Initialize the communication with the panel
        send(String.format("RMT=%s", password)); // REMOTE
        send("LCL"); // LOCAL

        logger.trace("RiscoCommunicator communication threads started successfully");
    }

    public void start() throws IOException {
        logger.debug("start(): RiscoCommunicator stopping");
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

        // Start watchdog
        riscoWatchdog = scheduler.scheduleWithFixedDelay(new RiscoWatchdog(), 10, 20, TimeUnit.SECONDS);

        connected = true;

        // Initialize the communication with the panel
        send(String.format("RMT=%s", password)); // REMOTE
        send("LCL"); // LOCAL
    }

    public void stop() {
        logger.debug("stop(): RiscoCommunicator stopping");

        connected = false;

        // Disconnect command
        send("DCN");

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
            logger.debug("closeConnection(): Closed TCP Connection!");
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

        // Stop watchdog
        riscoWatchdog.cancel(true);
    }

    public synchronized void send(String command) {
        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg = factory.create(panelId, encoding, sendCommandId, command, true);
        // RiscoMessagePair pair = new RiscoMessagePair(msg);

        // adjust command id For next send (1-45)
        sendCommandId++;
        if (sendCommandId == 46) {
            sendCommandId = 1;
        }

        // inFlightQueue.add(pair);
        sendQueue.add(msg);
    }

    public void sendFirst(int commandId, String command) {
        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg = factory.create(panelId, encoding, commandId, command, true);

        sendQueue.add(msg);
    }

    private void handleIncomingMessage(RiscoMessage msg) {
        logger.debug("<---- {}", msg);

        lastReceiveTime = ZonedDateTime.now();

        if (msg.getMessageOrigin() == MessageOrigin.PANEL) {
            sendFirst(msg.getCommandId(), "ACK");
        } else if (msg.getMessageOrigin() == MessageOrigin.BINDING) {
            for (RiscoPanelListener listener : listenerQueue) {
                listener.handleRiscoMessage(msg);
            }
        } else {
            // Unknown message origin
            logger.debug("Unknown message origin. Abnormal situation. {}", msg);
        }
    }

    private void handleOutgoingMessage(RiscoMessage msg) {
        logger.debug("----> {}", msg);

        lastSendTime = ZonedDateTime.now();

        if (msg.getMessageOrigin() == MessageOrigin.PANEL) {
            for (RiscoPanelListener listener : listenerQueue) {
                listener.handleRiscoMessage(msg);
            }
        } else if (msg.getMessageOrigin() == MessageOrigin.BINDING) {
            // Nothing to be done
        } else {
            // Unknown message origin
            logger.debug("Unknown message origin. Abnormal situation. {}", msg);
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

            logger.debug("RiscoReceiver. Thread stopped.");
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

                logger.debug("RiscoCommunicator.SenderThread: Thread interrupted.");
            } catch (InterruptedException e) {
                // Just exit the loop
                logger.debug("RiscoCommunicator.SenderThread: InterruptedException caught.");
            } catch (IOException e) {
                logger.debug("RiscoCommunicator.SenderThread: IOException caught.", e);
            }

            logger.debug("RiscoSender. Thread stopped.");
        }

        private void write(byte[] buffer) throws IOException {
            tcpOutput.write(buffer);
            tcpOutput.flush();
            logger.trace("write(): Message Sent: {}", buffer);
        }
    }

    private void reconnect() throws IOException {
        stop();
        start();
    }

    private class RiscoWatchdog implements Runnable {
        @Override
        public void run() {
            if (ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()) > 30
                    || ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()) > 70) {
                logger.debug("check sendBefore: {}, recvBefore: {}, result: {}",
                        ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()),
                        ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()), "--");

                logger.debug("Reconnecting");
                try {
                    lastSendTime = ZonedDateTime.now();
                    lastReceiveTime = ZonedDateTime.now();
                    reconnect();
                } catch (IOException e) {
                    logger.warn("Could not reconnect to the panel.", e);
                }
                return;
            } else {
                logger.debug("check sendBefore: {}, recvBefore: {}, result: {}",
                        ChronoUnit.SECONDS.between(lastSendTime, ZonedDateTime.now()),
                        ChronoUnit.SECONDS.between(lastReceiveTime, ZonedDateTime.now()), "OK");
            }

            send("CLOCK");
        }
    }
}
