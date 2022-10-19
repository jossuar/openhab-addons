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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
    private final BlockingDeque<RiscoMessage> sendQueue = new LinkedBlockingDeque<>(50);
    private int sendCommandId = 0;

    // receive
    private final Thread riscoReceiver;

    // in-flight
    private final BlockingDeque<RiscoMessagePair> inFlightQueue = new LinkedBlockingDeque<>();

    // listener
    private final Set<RiscoPanelListener> listenerQueue = new HashSet<>();

    public interface RiscoPanelListener {
        public void handleRiscoMessage(RiscoMessagePair pair);
    }

    public void addListener(RiscoPanelListener listener) {
        listenerQueue.add(listener);
    }

    public RiscoCommunicator(String uid, String hostname, int port, int panelId, String encoding) throws IOException {
        logger.debug("openConnection(): Connecting to Risco panel");

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
        logger.debug("RiscoCommunicator stopping");

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

        connected = false;
    }

    public synchronized void send(String command) {
        RiscoMessage msg = new RiscoMessage(panelId, encoding, sendCommandId, command, true);
        RiscoMessagePair pair = new RiscoMessagePair(msg);

        // adjust command id For next send (0-49)
        sendCommandId++;
        if (sendCommandId == 50) {
            sendCommandId = 0;
        }

        inFlightQueue.add(pair);
        sendQueue.add(msg);
    }

    public void sendFirst(int commandId, String command) {
        RiscoMessage msg = new RiscoMessage(panelId, encoding, commandId, command, true);

        sendQueue.addFirst(msg);
    }

    @SuppressWarnings({ "null", "unused" })
    private void handleIncomingMessage(RiscoMessage msg) {
        logger.debug("<---- {}", msg);

        if (msg.getMessageOrigin() == MessageOrigin.PANEL) {
            sendFirst(msg.getCommandId(), "ACK");

            RiscoMessagePair pair = new RiscoMessagePair(msg);
            inFlightQueue.add(pair);
        } else if (msg.getMessageOrigin() == MessageOrigin.BINDING) {
            RiscoMessagePair m = findInInFlightQueue(msg.getCommandId());
            if (m != null) {
                m.setResponse(msg);
            }

            while ((m = inFlightQueue.peek()) != null) {
                if (!m.hasResponse()) {
                    break;
                }

                m = inFlightQueue.poll();
                for (RiscoPanelListener listener : listenerQueue) {
                    listener.handleRiscoMessage(m);
                }
            }

        } else {
            // Unknown message origin
            logger.debug("Unknown message origin. Abnormal situation. {}", msg);
        }
    }

    @SuppressWarnings({ "null", "unused" })
    private void handleOutgoingMessage(RiscoMessage msg) {
        logger.debug("------> {}", msg);

        if (msg.getMessageOrigin() == MessageOrigin.PANEL) {
            RiscoMessagePair m = findInInFlightQueue(msg.getCommandId());
            if (m != null) {
                m.setResponse(msg);
            }

            while ((m = inFlightQueue.poll()) != null) {
                if (!m.hasResponse()) {
                    break;
                }

                for (RiscoPanelListener listener : listenerQueue) {
                    logger.trace("Informing listener: {}", listener);
                    listener.handleRiscoMessage(m);
                }
            }

        } else if (msg.getMessageOrigin() == MessageOrigin.BINDING) {
            // Nothing to be done
        } else {
            // Unknown message origin
            logger.debug("Unknown message origin. Abnormal situation. {}", msg);
        }
    }

    private @Nullable RiscoMessagePair findInInFlightQueue(int commandId) {
        RiscoMessagePair msg = null;
        Iterator<RiscoMessagePair> itr = inFlightQueue.iterator();

        while (itr.hasNext()) {
            msg = itr.next();
            if (msg.getRequest().getCommandId() == commandId) {
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
                RiscoMessage rm = new RiscoMessage(1, "UTF-8", message);
                handleIncomingMessage(rm);

                logger.trace("RiscoCommunicator.read() Got message");
            } catch (EOFException e) {
                return;
            } catch (IOException e) {
                logger.debug("IOException caught.");
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
                    RiscoMessage rm = sendQueue.take();
                    write(rm.getEncryptedMessage());

                    handleOutgoingMessage(rm);
                }

                logger.debug("RiscoCommunicator.SenderThread: Thread interrupted.");
            } catch (InterruptedException e) {
                // Just exit the loop
                logger.debug("RiscoCommunicator.SenderThread: InterruptedException caught.");
            } catch (IOException e) {
                logger.debug("RiscoCommunicator.SenderThread: IOException caught. {}", e);
            }

            logger.debug("RiscoSender. Thread stopped.");
        }

        private void write(byte[] buffer) throws IOException {
            tcpOutput.write(buffer);
            tcpOutput.flush();
            logger.trace("write(): Message Sent: {}", buffer);
        }
    }
}
