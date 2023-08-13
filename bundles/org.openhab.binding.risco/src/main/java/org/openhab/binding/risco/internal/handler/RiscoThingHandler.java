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
package org.openhab.binding.risco.internal.handler;

import java.util.Optional;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.protocol.RiscoProperty;
import org.openhab.binding.risco.internal.protocol.RiscoThing;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public abstract class RiscoThingHandler extends BaseThingHandler implements RiscoThingEvent {
    private final Logger logger = LoggerFactory.getLogger(RiscoThingHandler.class);

    private @Nullable RiscoBridgeHandler bridgeHandler;

    public RiscoThingHandler(Thing thing) {
        super(thing);
    }

    protected @Nullable RiscoBridgeHandler getBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();

            if (bridge == null) {
                logger.debug("getBridgeHandler(): Unable to get bridge!");
                return null;
            }

            logger.trace("getBridgeHandler(): Bridge for '{}' - '{}'", getThing().getUID(), bridge.getUID());

            ThingHandler handler = bridge.getHandler();

            if (handler instanceof RiscoBridgeHandler) {
                this.bridgeHandler = (RiscoBridgeHandler) handler;
            } else {
                logger.debug("getBridgeHandler(): Unable to get bridge handler!");
            }
        }

        return this.bridgeHandler;
    }

    protected void updateChannel(String channelID, String data) {
        logger.trace("Updating zone channel: {}, {}", channelID, data);

        Optional<Channel> channel = getThing().getChannels().stream().filter(c -> c.getUID().getId().equals(channelID))
                .findAny();
        if (channel.isPresent()) {
            ChannelTypeUID channelTypeUID = channel.get().getChannelTypeUID();
            if (channelTypeUID != null) {
                String channelTypeID = channelTypeUID.getId();

                if (RiscoBindingConstants.CHANNEL_TYPES_SWITCH.contains(channelTypeID)) {
                    OnOffType onOffType = ("true".equals(data)) ? OnOffType.ON : OnOffType.OFF;
                    updateState(channelID, onOffType);

                    logger.trace("Switch [{}] updated", channelID);
                } else if (RiscoBindingConstants.CHANNEL_TYPES_CONTACT.contains(channelTypeID)) {
                    OpenClosedType openClosedType = ("true".equals(data)) ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
                    updateState(channelID, openClosedType);

                    logger.trace("Contact [{}] updated", channelID);
                } else if (RiscoBindingConstants.CHANNEL_TYPES_TEXT.contains(channelTypeID)) {
                    updateState(channelID, new StringType(data));

                    logger.trace("Text [{}] updated", channelID);
                }
            }
        } else {
            logger.trace("Channel {} is not present", channelID);
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
}
