/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.mysensors.internal.protocol.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.internal.event.MySensorsEventRegister;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGatewayConfig;
import org.openhab.binding.mysensors.internal.protocol.MySensorsAbstractConnection;
import org.openhab.core.io.transport.serial.*;

/**
 * Connection to the serial interface where the MySensors Gateway is connected.
 *
 * @author Tim Oberföll - Initial contribution
 * @author Andrea Cioni - Redesign
 *
 */
@NonNullByDefault
public class MySensorsSerialConnection extends MySensorsAbstractConnection implements SerialPortEventListener {
    @Nullable
    private SerialPort serialConnection;
    private final SerialPortManager serialPortManager;

    public MySensorsSerialConnection(MySensorsGatewayConfig myConf, MySensorsEventRegister myEventRegister,
            SerialPortManager serialPortManager) {
        super(myConf, myEventRegister);
        this.serialPortManager = serialPortManager;
    }

    /**
     * Tries to accomplish a connection via a serial port to the serial gateway.
     */
    @Override
    public boolean establishConnection() {
        logger.debug("Connecting to {} [baudRate:{}]", myGatewayConfig.getSerialPort(), myGatewayConfig.getBaudRate());
        String serialPort = myGatewayConfig.getSerialPort();
        int baudRate = myGatewayConfig.getBaudRate();

        try {
            @Nullable
            SerialPortIdentifier portIdentifier = this.serialPortManager.getIdentifier(serialPort);
            if (portIdentifier == null) {
                throw new IllegalStateException("Serial Port Identifier not found");
            }
            serialConnection = portIdentifier.open(getClass().getName(), 2000);
            serialConnection.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialConnection.enableReceiveThreshold(1);
            serialConnection.enableReceiveTimeout(100); // In ms. Small values mean faster shutdown but more cpu

            @Nullable
            InputStream inStream = serialConnection.getInputStream();
            @Nullable
            OutputStream outStream = serialConnection.getOutputStream();

            // RXTX serial port library causes high CPU load
            // Start event listener, which will just sleep and slow down event loop
            try {
                serialConnection.addEventListener(this);
                serialConnection.notifyOnDataAvailable(true);
                logger.debug("Serial port event listener started");
            } catch (TooManyListenersException e) {
                logger.warn("Exception trying to add listener ", e);
            }

            logger.debug("Successfully connected to serial port.");

            if (inStream == null || outStream == null) {
                logger.error("Cannot start thread writer because input or output are null");
                return false;
            }
            mysConReader = new MySensorsReader(inStream);
            mysConWriter = new MySensorsWriter(outStream);

            return startReaderWriterThread(mysConReader, mysConWriter);
        } catch (PortInUseException e) {
            logger.error("Port: {} is already in use", myGatewayConfig.getSerialPort(), e);
        } catch (UnsupportedCommOperationException e) {
            logger.error("Comm operation on port: {} not supported", myGatewayConfig.getSerialPort(), e);
        } catch (IOException e) {
            logger.error("IOException on port: {}", myGatewayConfig.getSerialPort(), e);
        } catch (Exception e) {
            logger.error("Exception found", e);
        }
        return false;
    }

    /**
     * Initiates a clean disconnect from the serial gateway.
     */
    @Override
    public void stopConnection() {
        logger.debug("Shutting down serial connection!");

        if (mysConWriter != null) {
            mysConWriter.stopWriting();
            mysConWriter = null;
        }

        if (mysConReader != null) {
            mysConReader.stopReader();
            mysConReader = null;
        }

        if (myGatewayConfig.isHardReset()) {
            resetAttachedGateway();
        }

        if (serialConnection != null) {
            try {
                serialConnection.removeEventListener();
                serialConnection.close();
            } catch (Exception e) {
                logger.warn("Error removing Serial Connection listener", e);
            }
            serialConnection = null;
        }
    }

    @Override
    public void serialEvent(SerialPortEvent arg0) {
        try {
            /*
             * See more details from
             * https://github.com/NeuronRobotics/nrjavaserial/issues/22
             */
            logger.trace("RXTX library CPU load workaround, sleep forever");
            Thread.sleep(Long.MAX_VALUE);
        } catch (InterruptedException e) {
            logger.warn("RXTX library CPU load workaround, sleep forever", e);
        }
    }

    /**
     * Try to reset the attached gateway by using DTR
     *
     */
    public void resetAttachedGateway() {
        logger.debug("Trying to reset of attached gateway with DTR");
        if (serialConnection == null) {
            logger.warn("Cannot resetAttachedGateway with null serialConnection");
            return;
        }
        serialConnection.setDTR(true);
        try {
            Thread.sleep(RESET_TIME_IN_MILLISECONDS);
        } catch (InterruptedException e) {
            logger.warn("Wait for reset of attached gateway interrupted!", e);
        }
        serialConnection.setDTR(false);
        logger.debug("Finished reset of attached gateway with DTR");
    }

    @Override
    public String toString() {
        return "MySensorsSerialConnection [serialPort=" + myGatewayConfig.getSerialPort() + ", baudRate="
                + myGatewayConfig.getBaudRate() + "]";
    }
}
