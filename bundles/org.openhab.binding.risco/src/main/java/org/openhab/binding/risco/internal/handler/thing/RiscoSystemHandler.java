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
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.handler.RiscoThingHandler;
import org.openhab.binding.risco.internal.protocol.RiscoProperty;
import org.openhab.binding.risco.internal.protocol.RiscoThing;
import org.openhab.binding.risco.internal.protocol.message.PanelConfiguration;
import org.openhab.binding.risco.internal.protocol.message.SystemStatus;
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
 * The {@link RiscoSystemHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoSystemHandler extends RiscoThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RiscoSystemHandler.class);

    public RiscoSystemHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.OFFLINE);

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        bridgeHandler.sendCommand(PanelConfiguration.getReadCommand());
        bridgeHandler.sendCommand(SystemStatus.getReadCommand());
        logger.trace("RiscoSystemHandler initialized");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        List<String> messages = new ArrayList<String>();

        if (command instanceof RefreshType) {
            messages.add(PanelConfiguration.getReadCommand());
            messages.add(SystemStatus.getReadCommand());
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
        logger.trace("ZoneHandler received info: {} {}", riscoThing.getRiscoThingType(), riscoThing.getIndex());

        for (RiscoProperty p : riscoThing.getProperties()) {
            updateChannel(p.getName(), p.getValue());
        }

        updateStatus(ThingStatus.ONLINE);
    }

    public void updateChannel(String channelID, String data) {
        logger.trace("Updating system channel: {}, {}", channelID, data);

        if (RiscoBindingConstants.SYSTEM_CHANNEL_NAME.equals(channelID)) {
            updateState(channelID, new StringType(data));

            logger.trace("  updateChannel: {} = {}", channelID, data);
        } else {
            OnOffType onOffType = ("true".equals(data)) ? OnOffType.ON : OnOffType.OFF;
            updateState(channelID, onOffType);

            logger.trace("  updateChannel: {} = {}", channelID, onOffType);
        }
    }
}
