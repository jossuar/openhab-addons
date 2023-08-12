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
package org.openhab.binding.risco.internal.handler.thing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.config.RiscoPartitionConfiguration;
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.handler.RiscoThingHandler;
import org.openhab.binding.risco.internal.protocol.RiscoProperty;
import org.openhab.binding.risco.internal.protocol.RiscoThing;
import org.openhab.binding.risco.internal.protocol.message.PartitionLabel;
import org.openhab.binding.risco.internal.protocol.message.PartitionStatus;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
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
            messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
            messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));
        } else {
            logger.debug("Unknown command {}", command);
            return;
        }

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Send Zone Status update command
        for (String m : messages) {
            bridgeHandler.sendCommand(m);
        }
    }

    @Override
    public void handleEvent(RiscoThing riscoThing) {
        logger.trace("PartitionHandler received info: {} {}", riscoThing.getRiscoThingType(), riscoThing.getIndex());

        for (RiscoProperty p : riscoThing.getProperties()) {
            updateChannel(p.getName(), p.getValue());
        }

        updateStatus(ThingStatus.ONLINE);
    }

    public void updateChannel(String channelID, String data) {
        logger.trace("Updating Partition channel: {}, {}", channelID, data);

        if (RiscoBindingConstants.PARTITION_CHANNEL_NAME.equals(channelID)) {
            updateState(channelID, new StringType(data));

            logger.trace("  updateChannel: {} = {}", channelID, data);
        } else {
            OnOffType onOffType = ("true".equals(data)) ? OnOffType.ON : OnOffType.OFF;
            updateState(channelID, onOffType);

            logger.trace("  updateChannel: {} = {}", channelID, onOffType);
        }
    }
}
