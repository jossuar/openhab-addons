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
package org.openhab.binding.mysensors.internal.protocol.ip;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.internal.event.MySensorsEventRegister;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGatewayConfig;
import org.openhab.binding.mysensors.internal.protocol.MySensorsAbstractConnection;

/**
 * Implements the TCP/IP connection to the ethernet gateway of the MySensors network.
 *
 * @author Andrea Cioni - Initial contribution
 * @author Tim Oberföll - Redesign
 *
 */
@NonNullByDefault
public class MySensorsIpConnection extends MySensorsAbstractConnection {
    @Nullable
    private Socket sock = null;

    public MySensorsIpConnection(MySensorsGatewayConfig myConf, MySensorsEventRegister myEventRegister) {
        super(myConf, myEventRegister);
    }

    /**
     * Tries to accomplish a TCP/IP connection via socket to ethernet gateway.
     */
    @Override
    public boolean establishConnection() {
        logger.debug("Connecting to IP bridge [{}:{}]", myGatewayConfig.getIpAddress(), myGatewayConfig.getTcpPort());
        try {
            sock = new Socket(myGatewayConfig.getIpAddress(), myGatewayConfig.getTcpPort());
            mysConReader = new MySensorsReader(sock.getInputStream());
            mysConWriter = new MySensorsWriter(sock.getOutputStream());

            return startReaderWriterThread(mysConReader, mysConWriter);
        } catch (UnknownHostException e) {
            logger.error("Error while trying to connect to: {}:{}", myGatewayConfig.getIpAddress(),
                    myGatewayConfig.getTcpPort(), e);
            return false;
        } catch (IOException e) {
            logger.error("Error while trying to connect InputStreamReader", e);
            return false;
        }
    }

    /**
     * Ensures a clean disconnect from the TCP/IP connection to the gateway.
     */
    @Override
    public void stopConnection() {
        logger.debug("Disconnecting from IP bridge ...");

        if (mysConWriter != null) {
            mysConWriter.stopWriting();
            mysConWriter = null;
        }

        if (mysConReader != null) {
            mysConReader.stopReader();
            mysConReader = null;
        }

        // Shut down socket
        try {
            if (sock != null && sock.isConnected()) {
                sock.close();
                sock = null;
            }
        } catch (IOException e) {
            logger.error("cannot disconnect from socket, message: {}", e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "MySensorsIpConnection [ipAddress=" + myGatewayConfig.getIpAddress() + ", tcpPort="
                + myGatewayConfig.getTcpPort() + "]";
    }
}
