/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.risco.internal.handler;

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoCommunicator;
import org.openhab.binding.risco.internal.config.RiscoBridgeConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoBridgeHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(RiscoBridgeHandler.class);

    public RiscoBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    private String hostName = "";
    private int port;
    private @Nullable RiscoCommunicator communicator = null;

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);

        RiscoBridgeConfiguration configuration = getConfigAs(RiscoBridgeConfiguration.class);

        String panelIp = configuration.getHostname();
        if (panelIp == null || panelIp.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Set an IP address in the thing configuration.");

            return;
        }

        hostName = panelIp;
        port = configuration.getPort();
        updateStatus(ThingStatus.OFFLINE);

        // create & start panel interface
        logger.debug("Starting interface with host {} at port {}", hostName, port);

        try {
            communicator = new RiscoCommunicator(getThing().getUID().getAsString(), hostName, port,
                    configuration.getId(), configuration.getEncoding());
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication cannot be initialized. " + e.toString());

            return;
        }

        // RiscoCommunicator comm = communicator;
        // if (comm != null) {
        // comm.addListener(this);
        // }
        updateStatus(ThingStatus.ONLINE);

        // list all channels
        if (logger.isTraceEnabled()) {
            logger.trace("list all {} channels:", getThing().getChannels().size());
            for (Channel c : getThing().getChannels()) {
                logger.trace("Channel Type {} UID {}", c.getChannelTypeUID(), c.getUID());
            }
        }
    }

    @Override
    public void dispose() {
        RiscoCommunicator comm = communicator;
        if (comm != null) {
            comm.stop();
            comm = null;
        }

        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("handleCommand(), channelUID: {}, command: {}", channelUID, command);

        switch (channelUID.getId()) {
            default:
                logger.debug("Unknown command {}", command);
                break;
        }
    }

    public void restart() {
        // Stop the currently running communicator
        RiscoCommunicator comm = communicator;
        if (comm != null) {
            comm.stop();
            comm = null;
        }

        // Initialize again
        initialize();
    }
}
