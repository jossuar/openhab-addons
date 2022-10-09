package org.openhab.binding.risco.internal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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

    private final int panelId;
    private final String encoding;
    private final Socket tcpSocket;
    private final BufferedOutputStream tcpOutput;
    private final BufferedInputStream tcpInput;
    private Boolean connected = false;

    // send
    private final Thread riscoSender;
    private final BlockingDeque<RiscoMessage> sendQueue = new LinkedBlockingDeque<RiscoMessage>(50);
    private int sendCommandId = 0;

    // receive
    private final Thread riscoReceiver;
    private final BlockingDeque<RiscoMessage> receiveQueue = new LinkedBlockingDeque<>();

    public RiscoCommunicator(String uid, String hostname, int port, int panelId, String encoding) throws IOException {
        logger.warn("openConnection(): Connecting to Risco panel");

        this.panelId = panelId;
        this.encoding = encoding;

        // Open the socket and get the streams
        tcpSocket = new Socket();
        SocketAddress socketAddress = new InetSocketAddress(hostname, port);
        tcpSocket.connect(socketAddress, 5000);
        tcpOutput = new BufferedOutputStream(tcpSocket.getOutputStream());
        tcpInput = new BufferedInputStream(tcpSocket.getInputStream());

        // Start receiver and sender threads
        riscoReceiver = new Thread(new RiscoReceiver(), "OH-binding-" + uid + "-riscoreceiver");
        riscoReceiver.setDaemon(true);
        riscoReceiver.start();

        riscoSender = new Thread(new RiscoSender(), "OH-binding-" + uid + "-riscosender");
        riscoSender.setDaemon(true);
        riscoSender.start();

        connected = true;

        logger.trace("RiscoCommunicator communication threads started successfully");
    }

    public void stop() {
        logger.trace("RiscoCommunicator stopping");

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
            logger.warn("closeConnection(): Closed TCP Connection!");
        } catch (IOException ioException) {
            logger.error("closeConnection(): Unable to close connection - {}", ioException.getMessage());
        } catch (Exception exception) {
            logger.error("closeConnection(): Error closing connection - {}", exception.getMessage());
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

        connected = false;
    }

    public void send(String command) {
        RiscoMessage msg = new RiscoMessage(panelId, encoding, sendCommandId, command, true);
        sendQueue.add(msg);

        // adjust command id For next send (0-49)
        sendCommandId++;
        if (sendCommandId == 50) {
            sendCommandId = 0;
        }
    }

    public void sendFirst(int commandId, String command) {
        RiscoMessage msg = new RiscoMessage(panelId, encoding, commandId, command, true);

        sendQueue.addFirst(msg);
    }

    private void handleIncomingMessage(RiscoMessage msg) {
        logger.warn("Handle message {} - {}", msg.getCommandId(), msg.getCommand());

        Integer id = msg.getCommandId();
        if (id != null) {
            if (id >= 50) {
                sendFirst(id, "ACK");

                receiveQueue.add(msg);
            } else {
                RiscoMessage m = findInSendQueue(msg.getCommandId());
                if (m == null) {
                    return;
                }

            }
        }

        msg.getCommandId();
    }

    private @Nullable RiscoMessage findInSendQueue(@Nullable Integer commandId) {
        RiscoMessage msg = null;
        Iterator<RiscoMessage> itr = sendQueue.iterator();

        while (itr.hasNext()) {
            msg = itr.next();
            if (msg.getCommandId() == commandId) {
                break;
            }
        }

        return msg;
    }

    public Boolean isConnected() {
        return connected;
    }

    private class RiscoReceiver implements Runnable {
        private final int MAX_MESSAGE_SIZE = 4096;
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

            logger.warn("RiscoReceiver. Thread stopped.");
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
                        logger.trace("Message exceeded {} bytes.", MAX_MESSAGE_SIZE);

                        // Stop the receiver
                        Thread.currentThread().interrupt();
                    }

                    if (b == 0x03 && !stuffed) {
                        break;
                    }
                } while (true);

                byte[] message = Arrays.copyOfRange(buffer, 0, bufferIndex);
                RiscoMessage rm = new RiscoMessage(1, "UTF-8", message);
                handleIncomingMessage(rm);

                logger.trace("RiscoCommunicator.read() Got message");
            } catch (EOFException e) {
                return;
            } catch (IOException e) {
                logger.trace("IOException caught.");
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
        /**
         * Run method. Runs the MessageListener thread
         */
        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    RiscoMessage outgoingMessage = sendQueue.take();
                    write(outgoingMessage.getEncryptedMessage());

                    logger.warn("->: {}", outgoingMessage);
                }

                logger.warn("RiscoCommunicator.SenderThread: Thread interrupted.");
            } catch (InterruptedException e) {
                // Just exit the loop
                logger.warn("RiscoCommunicator.SenderThread: InterruptedException caught.");
            } catch (IOException e) {
                logger.warn("RiscoCommunicator.SenderThread: IOException caught. {}", e);
            }

            logger.warn("RiscoSender. Thread stopped.");
        }

        private void write(byte[] buffer) throws IOException {
            tcpOutput.write(buffer);
            tcpOutput.flush();
            logger.warn("write(): Message Sent: {}", buffer);
        }
    }
}
