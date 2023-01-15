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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.config.RiscoZoneConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoZoneHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoZoneHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RiscoZoneHandler.class);

    private int zoneNumber;

    private @Nullable RiscoBridgeHandler bridgeHandler;

    public RiscoZoneHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        // Load configuration
        RiscoZoneConfiguration config = getConfigAs(RiscoZoneConfiguration.class);
        setZoneNumber(config.getZoneNumber());

        // set the Thing offline for now
        updateStatus(ThingStatus.OFFLINE);

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        //
        bridgeHandler.sendCommand("ZSTT" + getZoneNumber() + "?");
    }

    public @Nullable RiscoBridgeHandler getBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();

            if (bridge == null) {
                logger.debug("getCaddxBridgeHandler(): Unable to get bridge!");
                return null;
            }

            logger.trace("getCaddxBridgeHandler(): Bridge for '{}' - '{}'", getThing().getUID(), bridge.getUID());

            ThingHandler handler = bridge.getHandler();

            if (handler instanceof RiscoBridgeHandler) {
                this.bridgeHandler = (RiscoBridgeHandler) handler;
            } else {
                logger.debug("getBridgeHandler(): Unable to get bridge handler!");
            }
        }

        return this.bridgeHandler;
    }

    public void updateChannel(ChannelUID channelUID, String data) {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    public @Nullable Channel getChannel(ChannelUID channelUID) {
        Channel channel = null;

        List<Channel> channels = getThing().getChannels();

        for (Channel ch : channels) {
            if (channelUID.equals(ch.getUID())) {
                channel = ch;
                break;
            }
        }

        return channel;
    }

    public int getZoneNumber() {
        return zoneNumber;
    }

    public void setZoneNumber(int zoneNumber) {
        this.zoneNumber = zoneNumber;
    }
}
