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
package org.openhab.binding.caddx.internal.discovery;

import java.io.IOException;
import java.util.Enumeration;
import java.util.TooManyListenersException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.caddx.internal.CaddxCommunicator;
import org.openhab.binding.caddx.internal.CaddxCommunicator.SecurityPanelListener;
import org.openhab.binding.caddx.internal.CaddxMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

/**
 * This class is responsible for discovering the Caddx RS232 Serial interface.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class CaddxBridgeDiscovery implements SecurityPanelListener {
    private final Logger logger = LoggerFactory.getLogger(CaddxBridgeDiscovery.class);

    static final int[] BAUDRATES = { 9600, 19200, 38400, 57600, 115200 };
    static final byte[] CADDX_DISCOVERY_INTERFACE_CONFIGURATION_MESSAGE = { 0x21 };

    private CaddxDiscoveryService caddxDiscoveryService;

    /**
     * Constructor.
     */
    public CaddxBridgeDiscovery(CaddxDiscoveryService caddxDiscoveryService) {
        this.caddxDiscoveryService = caddxDiscoveryService;
    }

    public synchronized void discoverBridge3() {
        if (logger.isTraceEnabled()) {
            logger.trace("Starting Caddx Bridge Discovery.");
        }

        Enumeration<?> ports = CommPortIdentifier.getPortIdentifiers();
        CaddxCommunicator caddxCommunicator = null;
        while (ports.hasMoreElements()) {
            CommPortIdentifier portIdentifier = (CommPortIdentifier) ports.nextElement();
            if (portIdentifier == null) {
                continue;
            }

            if (portIdentifier.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                try {
                    for (int baudrate : BAUDRATES) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Checking port: {}, baud: {}", portIdentifier.getName(), baudrate);
                        }
                        caddxCommunicator = new CaddxCommunicator(portIdentifier.getName(), baudrate);
                        caddxCommunicator.addListener(this);
                        caddxCommunicator
                                .transmit(new CaddxMessage(CADDX_DISCOVERY_INTERFACE_CONFIGURATION_MESSAGE, false));

                        try {
                            TimeUnit.SECONDS.sleep(4); // Wait for 4 seconds and continue with next baudrate/port
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        caddxCommunicator.stop();
                    }
                } catch (UnsupportedCommOperationException | NoSuchPortException | PortInUseException | IOException
                        | TooManyListenersException e1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Port: {} is not applicable.", portIdentifier.getName());
                    }
                    continue;
                }

                // if the thread was interrupted then exit the discovery loop
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
            }
        }
    }

    @Override
    public void caddxMessage(CaddxCommunicator communicator, CaddxMessage caddxMessage) {
        if (logger.isTraceEnabled()) {
            logger.trace("Received message. [0x{}] on {} {}", String.format("%02x", caddxMessage.getMessageType()),
                    communicator.getSerialPortName(), communicator.getBaudRate());
        }

        caddxDiscoveryService.addCaddxBridge(communicator.getSerialPortName(), communicator.getBaudRate());
    }
}
