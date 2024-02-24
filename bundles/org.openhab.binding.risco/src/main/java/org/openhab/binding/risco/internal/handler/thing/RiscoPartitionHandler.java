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
package org.openhab.binding.risco.internal.handler.thing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.config.RiscoPartitionConfiguration;
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.handler.RiscoThingHandler;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionArm;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionDisarm;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionLabel;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionStay;
import org.openhab.binding.risco.internal.protocol.message.status.PartitionStatus;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoPartitionHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoPartitionHandler extends RiscoThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RiscoPartitionHandler.class);

    private int partitionNumber;
    private long lastRefreshTime = 0;

    public RiscoPartitionHandler(Thing thing) {
        super(thing);
    }

    public int getPartitionNumber() {
        return partitionNumber;
    }

    public void setPartitionNumber(int zoneNumber) {
        this.partitionNumber = zoneNumber;
    }

    @Override
    public void initialize() {
        // Load configuration
        RiscoPartitionConfiguration config = getConfigAs(RiscoPartitionConfiguration.class);
        setPartitionNumber(config.getPartitionNumber());

        // set the Thing offline for now
        updateStatus(ThingStatus.OFFLINE);

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Send Zone Status update command
        bridgeHandler.sendCommand(PartitionStatus.getReadCommand(partitionNumber));
        bridgeHandler.sendCommand(PartitionLabel.getReadCommand(getPartitionNumber()));
        logger.trace("RiscoPartitionHandler initialized [{}]", partitionNumber);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        List<String> messages = new ArrayList<String>();

        if (command instanceof RefreshType) {
            // Refresh only if 5 seconds have passed from the last refresh
            if (System.currentTimeMillis() - lastRefreshTime > 5000) {
                messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
                messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));
                lastRefreshTime = System.currentTimeMillis();
            }
        } else if (channelUID.getId().equals(RiscoBindingConstants.PARTITION_CHANNEL_ARM)) {
            if (command instanceof OnOffType) {
                OnOffType oo = (OnOffType) command;
                if (oo == OnOffType.OFF) {
                    messages.add(PartitionArm.getCommand(getPartitionNumber()));
                    messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
                    messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));
                } else {
                    messages.add(PartitionDisarm.getCommand(getPartitionNumber()));
                    messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
                    messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));
                }
            }
        } else if (channelUID.getId().equals(RiscoBindingConstants.PARTITION_CHANNEL_HOME_STAY)) {
            if (command instanceof OnOffType) {
                OnOffType oo = (OnOffType) command;
                if (oo == OnOffType.OFF) {
                    messages.add(PartitionStay.getCommand(getPartitionNumber()));
                    messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
                    messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));
                } else {
                    messages.add(PartitionDisarm.getCommand(getPartitionNumber()));
                    messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
                    messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));
                }
            }
        } else {
            logger.debug("Unknown command channel:{} command:{}", channelUID, command);
            return;
        }

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        for (String m : messages) {
            bridgeHandler.sendCommand(m);
        }
    }
}
