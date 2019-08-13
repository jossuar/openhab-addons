/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.caddx.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.TooManyListenersException;
import java.util.concurrent.Exchanger;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * The {@link CaddxCommunicator} is responsible for the asynchronous serial communication
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class CaddxCommunicator implements Runnable, SerialPortEventListener {
    private final Logger logger = LoggerFactory.getLogger(CaddxCommunicator.class);

    private final ArrayList<SecurityPanelListener> listenerQueue = new ArrayList<>();

    private Thread thread;
    private final LinkedBlockingDeque<CaddxMessage> messages = new LinkedBlockingDeque<>();

    private CaddxProtocol protocol;
    private String serialPortName;
    private int baudRate;
    private SerialPort serialPort;
    private InputStream in;
    private OutputStream out;

    Exchanger<CaddxMessage> exchanger = new Exchanger<>();

    public interface SecurityPanelListener {
        public void caddxMessage(CaddxCommunicator communicator, CaddxMessage message);
    }

    public void addListener(SecurityPanelListener listener) {
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.addListener() Started");
        }

        if (!listenerQueue.contains(listener)) {
            listenerQueue.add(listener);
        }
    }

    public CaddxCommunicator(CaddxProtocol protocol, String serialPortName, int baudRate)
            throws UnsupportedCommOperationException, NoSuchPortException, PortInUseException, IOException,
            TooManyListenersException {
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator() Started {}", serialPortName);
        }

        this.protocol = protocol;
        this.serialPortName = serialPortName;
        this.baudRate = baudRate;

        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
        CommPort commPort = portIdentifier.open(this.getClass().getName(), 2000);

        serialPort = (SerialPort) commPort;
        serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        serialPort.enableReceiveThreshold(1);
        serialPort.disableReceiveTimeout();

        in = serialPort.getInputStream();
        out = serialPort.getOutputStream();

        serialPort.notifyOnDataAvailable(true);
        serialPort.addEventListener(this);

        thread = new Thread(this, "Caddx Communicator");
        thread.start();

        message = new byte[0];
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator communication thread started successfully for {}", serialPortName);
        }
    }

    public void stop() {
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.stop() Started");
        }

        // kick thread out of waiting for FIFO
        thread.interrupt();

        // Close the streams first to unblock blocked reads and writes
        try {
            in.close();
            out.close();
        } catch (IOException e) {
        }

        // Communication thread should now exit. Wait for 5 sec or not wait at all??
        while (thread.isAlive()) {
            ;
        }

        // Also close the serial port
        serialPort.removeEventListener();
        serialPort.close();
    }

    public CaddxProtocol getProtocol() {
        return protocol;
    }

    public String getSerialPortName() {
        return serialPortName;
    }

    public int getBaudRate() {
        return baudRate;
    }

    /**
     * Send message to panel. Asynchronous, i.e. returns immediately.
     * Messages are sent only when panel is ready (i.e. sent an
     * acknowledgment to last message), but no checks are implemented that
     * the message was correctly received and executed.
     *
     * @param msg Data to be sent to panel. First byte is message type.
     *            Fletcher sum is computed and appended by transmit.
     */
    public void transmit(CaddxMessage msg) { // byte... msg) {
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.transmit() Started");
        }

        messages.add(msg);
    }

    /**
     * Adds this message before any others in the queue.
     * Used by receiver to send ACKs.
     *
     * @param msg The message
     */
    public void transmitFirst(CaddxMessage msg) {
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.transmitFirst() Started");
        }

        messages.addFirst(msg);
    }

    @SuppressWarnings("null")
    @Override
    public void run() {
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.run() Started");
        }

        int @Nullable [] expectedMessageNumbers = null;

        @Nullable
        CaddxMessage outgoingMessage = null;
        boolean skipTransmit = false;

        try {
            // loop until the thread is interrupted, sending out messages
            while (!Thread.currentThread().isInterrupted()) {
                // Initialize the state
                outgoingMessage = null;
                expectedMessageNumbers = null;

                if (!skipTransmit) {
                    // send next outgoing message if we have one
                    outgoingMessage = messages.poll();
                    if (logger.isTraceEnabled() && outgoingMessage != null) {
                        logger.trace("CaddxCommunicator.run() Outgoing message: {}", outgoingMessage.getMessageType());
                    }

                    // Send the message
                    if (outgoingMessage != null) {
                        byte msg[] = outgoingMessage.getMessageFrameBytes(protocol);
                        out.write(msg);
                        out.flush();
                    }

                    // Log message
                    if (logger.isInfoEnabled() && outgoingMessage != null) {
                        logger.info("->: {}", outgoingMessage.getName());
                    }
                    if (logger.isDebugEnabled() && outgoingMessage != null) {
                        logger.debug("{}", Util.buildCaddxMessageInBinaryString("->: ", outgoingMessage));
                        // logger.debug("{}", Util.buildCaddxMessageInAsciiString("->: ", outgoingMessage));
                    }
                    if (outgoingMessage != null) {
                        expectedMessageNumbers = outgoingMessage.getReplyMessageNumbers();
                    }
                } else {
                    if (logger.isTraceEnabled()) {
                        logger.trace("CaddxCommunicator.run() skipTransmit: true");
                    }
                    skipTransmit = false;
                }

                // Check for an incoming message
                CaddxMessage incomingMessage = null;
                CaddxMessage throwAway = new CaddxMessage(new byte[] { 0x1d }, false);
                try {
                    incomingMessage = exchanger.exchange(throwAway, 3, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    if (expectedMessageNumbers == null) { // Nothing expected, Nothing received we continue
                        if (logger.isTraceEnabled()) {
                            logger.trace("CaddxCommunicator.run(): Nothing expected, Nothing received we continue");
                        }
                        continue;
                    }
                    logger.warn("CaddxCommunicator.run() TimeoutException caught.");
                }

                // Log
                if (logger.isInfoEnabled() && incomingMessage != null) {
                    logger.info("<-: {}", incomingMessage.getName());
                }
                if (logger.isDebugEnabled()) {
                    if (incomingMessage == null) {
                        logger.debug("CaddxCommunicator.run() NoMessage received.");
                    } else {
                        logger.debug("{}", Util.buildCaddxMessageInBinaryString("<-: ", incomingMessage));
                        // logger.debug("{}", Util.buildCaddxMessageInAsciiString("<-: ", incomingMessage));
                    }
                }

                // Check if we wait for a reply
                if (expectedMessageNumbers == null) {
                    if (incomingMessage != null) { // Nothing expected. Message received.
                        if (logger.isTraceEnabled()) {
                            logger.trace("CaddxCommunicator.run() Nothing expected, Message received");
                        }

                        // Check if Acknowledgement handling is required.
                        if (incomingMessage.hasAcknowledgementFlag()) {
                            if (incomingMessage.isChecksumCorrect()) {
                                messages.putFirst(new CaddxMessage(new byte[] { 0x1d }, false)); // send ACK
                            } else {
                                messages.putFirst(new CaddxMessage(new byte[] { 0x1d }, false)); // send ACK
                            }
                        }
                    }
                } else {
                    if (incomingMessage == null) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("CaddxCommunicator.run() Message expected. Nothing received");
                        }

                        // Message expected. Nothing received
                        if (outgoingMessage != null) {
                            messages.putFirst(outgoingMessage); // put message in queue again
                            continue;
                        }
                    } else {
                        if (logger.isTraceEnabled()) {
                            logger.trace("CaddxCommunicator.run() Message expected. Message received");
                        }

                        // Message expected. Message received.
                        int receivedMessageType = incomingMessage.getMessageType();
                        boolean isMessageExpected = IntStream.of(expectedMessageNumbers)
                                .anyMatch(x -> x == receivedMessageType);

                        if (!isMessageExpected) {
                            if (logger.isTraceEnabled()) {
                                logger.trace("Non expected message received exp:{}, recv: {}", expectedMessageNumbers,
                                        receivedMessageType);
                            }

                            // Non expected reply received
                            if (outgoingMessage != null) {
                                messages.putFirst(outgoingMessage); // put message in queue again
                                skipTransmit = true; // Skip the transmit on the next cycle to receive the panel message
                            }
                        }
                    }
                }

                // Inform the listeners
                if (incomingMessage != null && incomingMessage.isChecksumCorrect()) {
                    for (SecurityPanelListener listener : listenerQueue) {
                        listener.caddxMessage(this, incomingMessage);
                    }
                } else {
                    logger.warn(
                            "CaddxCommunicator.run() Received packet checksum does not match. in: {} {}, calc {} {}",
                            incomingMessage.getChecksum1In(), incomingMessage.getChecksum2In(),
                            incomingMessage.getChecksum1Calc(), incomingMessage.getChecksum2Calc());
                }
            }
        } catch (IOException e) {
            logger.warn("CaddxCommunicator.run() IOException. Stopping sender thread. {}", getSerialPortName());
            Thread.currentThread().interrupt();
        } catch (InterruptedException e) {
            logger.warn("CaddxCommunicator.run() InterruptedException. Stopping sender thread. {}",
                    getSerialPortName());
            Thread.currentThread().interrupt();
        }

        logger.warn("CaddxCommunicator.run() Sender thread stopped. {}", getSerialPortName());
    }

    // Receiver state variables
    private volatile boolean inMessage = false;
    private volatile boolean haveMessageLength = false;
    private volatile boolean haveFirstByte = false;
    private int messageBufferLength = 0;
    private byte[] message;
    private int messageBufferIndex = 0;
    private boolean unStuff = false;

    /**
     * Event handler to receive the data from the serial port
     *
     * @param SerialPortEvent serialPortEvent The event that occurred on the serial port
     */
    @Override
    public void serialEvent(@Nullable SerialPortEvent serialPortEvent) {
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.serialEvent() Started");
        }
        if (serialPortEvent == null) {
            return;
        }

        if (serialPortEvent.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            if (protocol == CaddxProtocol.Binary) {
                receiveInBinaryProtocol(serialPortEvent);
            } else {
                receiveInAsciiProtocol(serialPortEvent);
            }
        }
    }

    private void receiveInBinaryProtocol(SerialPortEvent serialPortEvent) {
        int b = 0;

        // Read the start byte
        if (!inMessage) // skip until 0x7E
        {
            b = 0;
            while (b != 0x7E && b != -1) {
                try {
                    b = in.read();
                } catch (IOException e) {
                    b = -1;
                }
            }
            if (b == -1) {
                return;
            }

            inMessage = true;
            messageBufferLength = 0;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.handleBinaryProtocol() Got start byte");
        }

        // Read the message length
        if (messageBufferLength == 0) {
            b = 0;
            try {
                b = in.read();
            } catch (IOException e) {
                b = -1;
            }
            if (b == -1) {
                return;
            }

            messageBufferLength = b + 2; // add two bytes for the checksum
            message = new byte[messageBufferLength];
        }
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.handleBinaryProtocol() Got message length {}", b);
        }

        // Read the message
        while (true) {
            b = 0;
            try {
                b = in.read();
            } catch (IOException e) {
                b = -1;
            }
            if (b == -1) {
                return;
            }
            message[messageBufferIndex] = (byte) b;

            if (message[messageBufferIndex] == 0x7D) {
                unStuff = true;
                continue;
            }

            if (unStuff) {
                message[messageBufferIndex] |= 0x20;
                unStuff = false;
            }

            messageBufferIndex++;
            if (messageBufferIndex == messageBufferLength) {
                break;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.handleBinaryProtocol() Got message {}", message[0]);
        }

        // Received data
        CaddxMessage caddxMessage = new CaddxMessage(message, true);

        try {
            if (logger.isTraceEnabled()) {
                logger.trace("CaddxCommunicator.handleBinaryProtocol() Exchanging");
            }

            exchanger.exchange(caddxMessage, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (logger.isTraceEnabled()) {
                logger.trace("CaddxCommunicator.handleBinaryProtocol() InterruptedException caught.");
            }
        } catch (TimeoutException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("CaddxCommunicator.handleBinaryProtocol() TimeoutException caught.");
            }
        }

        // Initialize state for next reception
        inMessage = false;
        haveMessageLength = false;
        haveFirstByte = false;
        messageBufferLength = 0;
        message = new byte[0];
        messageBufferIndex = 0;
        unStuff = false;
    }

    private void receiveInAsciiProtocol(SerialPortEvent serialPortEvent) {
        int b = 0;

        // Read the start byte
        if (!inMessage) // skip until 0x0A
        {
            b = 0;
            while (b != 0x0A && b != -1) {
                try {
                    b = in.read();
                } catch (IOException e) {
                    b = -1;
                }
            }
            if (b == -1) {
                return;
            }

            inMessage = true;
            haveMessageLength = false;
            haveFirstByte = false;
            messageBufferLength = 0;
        }
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.handleAsciiProtocol() Got start byte");
        }

        // Read the message length
        if (!haveMessageLength) {
            b = 0;
            while (!haveMessageLength) {
                try {
                    b = in.read();
                } catch (IOException e) {
                    b = -1;
                }
                if (b == -1) {
                    return;
                }

                if (!haveFirstByte) { // this is the 1st digit
                    b = b - 0x30;
                    messageBufferLength = b * 0x10;
                    haveFirstByte = true;
                } else {
                    b = b - 0x30;
                    messageBufferLength = messageBufferLength + b + 2;
                    haveMessageLength = true;
                    haveFirstByte = false;
                }
            }

            message = new byte[messageBufferLength];
        }
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.handleAsciiProtocol() Got message length {}", b);
        }

        // Read the message
        while (true) {
            b = 0;

            // Read the byte
            while (true) {
                try {
                    b = in.read();
                } catch (IOException e) {
                    b = -1;
                }
                if (b == -1) {
                    return;
                }

                if (!haveFirstByte) { // this is the 1st digit
                    b = b - 0x30;
                    message[messageBufferIndex] = (byte) (b * 0x10);
                    haveFirstByte = true;
                } else {
                    b = b - 0x30;
                    message[messageBufferIndex] = (byte) (message[messageBufferIndex] + b);
                    haveFirstByte = false;
                    break;
                }
            }

            messageBufferIndex++;
            if (messageBufferIndex == messageBufferLength) {
                break;
            }
        }
        if (logger.isTraceEnabled()) {
            logger.trace("CaddxCommunicator.handleAsciiProtocol() Got message {}", message[0]);
        }

        // Received data
        CaddxMessage caddxMessage = new CaddxMessage(message, true);

        try {
            if (logger.isTraceEnabled()) {
                logger.trace("CaddxCommunicator.serialEvent() Exchanging");
            }

            exchanger.exchange(caddxMessage, 3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (logger.isTraceEnabled()) {
                logger.trace("CaddxCommunicator.handleAsciiProtocol() InterruptedException caught.");
            }
        } catch (TimeoutException e) {
            if (logger.isTraceEnabled()) {
                logger.trace("CaddxCommunicator.handleAsciiProtocol() TimeoutException caught.");
            }
        }

        // Initialize state for next reception
        inMessage = false;
        haveMessageLength = false;
        haveFirstByte = false;
        messageBufferLength = 0;
        message = new byte[0];
        messageBufferIndex = 0;
        unStuff = false;
    }
}