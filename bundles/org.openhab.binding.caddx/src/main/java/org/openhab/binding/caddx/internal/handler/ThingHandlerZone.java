/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.caddx.internal.handler;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.caddx.internal.CaddxBindingConstants;
import org.openhab.binding.caddx.internal.CaddxEvent;
import org.openhab.binding.caddx.internal.CaddxMessage;
import org.openhab.binding.caddx.internal.CaddxMessageContext;
import org.openhab.binding.caddx.internal.CaddxMessageType;
import org.openhab.binding.caddx.internal.CaddxProperty;
import org.openhab.binding.caddx.internal.action.CaddxZoneActions;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a class for handling a Zone type Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ThingHandlerZone extends CaddxBaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ThingHandlerZone.class);
    private long lastRefreshTime = 0;

    public ThingHandlerZone(Thing thing) {
        super(thing, CaddxThingType.ZONE);
    }

    @Override
    public void updateChannel(ChannelUID channelUID, String data) {
        if (channelUID.getId().equals(CaddxBindingConstants.ZONE_NAME)) {
            getThing().setLabel(data);
            updateState(channelUID, new StringType(data));

            logger.trace("  updateChannel: {} = {}", channelUID, data);
        } else if (channelUID.getId().equals(CaddxBindingConstants.ZONE_FAULTED)) {
            OpenClosedType openClosedType = ("true".equals(data)) ? OpenClosedType.OPEN : OpenClosedType.CLOSED;
            updateState(channelUID, openClosedType);

            logger.trace("  updateChannel: {} = {}", channelUID, data);
        } else {
            OnOffType onOffType = ("true".equals(data)) ? OnOffType.ON : OnOffType.OFF;
            updateState(channelUID, onOffType);

            logger.trace("  updateChannel: {} = {}", channelUID, onOffType);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("handleCommand(): Command Received - {} {}.", channelUID, command);

        String cmd1 = null;
        String cmd2 = null;
        String data = null;

        if (command instanceof RefreshType) {
            // Refresh only if 2 seconds have passed from the last refresh
            if (System.currentTimeMillis() - lastRefreshTime > 2000) {
                cmd1 = CaddxBindingConstants.ZONE_STATUS_REQUEST;
                cmd2 = CaddxBindingConstants.ZONE_NAME_REQUEST;
                data = String.format("%d", getZoneNumber() - 1);
            } else {
                return;
            }
            lastRefreshTime = System.currentTimeMillis();
        } else if (channelUID.getId().equals(CaddxBindingConstants.ZONE_BYPASSED)) {
            cmd1 = CaddxBindingConstants.ZONE_BYPASSED;
            cmd2 = CaddxBindingConstants.ZONE_STATUS_REQUEST;
            data = String.format("%d", getZoneNumber() - 1);
        } else {
            logger.debug("Unknown command {}", command);
            return;
        }

        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }
        bridgeHandler.sendCommand(CaddxMessageContext.COMMAND, cmd1, data);
        bridgeHandler.sendCommand(CaddxMessageContext.COMMAND, cmd2, data);
    }

    @Override
    public void caddxEventReceived(CaddxEvent event, Thing thing) {
        logger.debug("Event Received - {}", event);

        if (getThing().equals(thing)) {
            CaddxMessage message = event.getCaddxMessage();
            CaddxMessageType mt = message.getCaddxMessageType();
            CaddxMessageContext mc = message.getContext();

            // Ignore Zone Status messages according to the configuration
            if (getIgnoreZoneStatusTransitions() && mt == CaddxMessageType.ZONE_STATUS_MESSAGE
                    && mc == CaddxMessageContext.NONE) {
                logger.debug("Zone {} Transition ignored.", getZoneNumber());
                return;
            }

            ChannelUID channelUID = null;

            for (CaddxProperty p : mt.properties) {
                logger.trace("  Checking property: {}", p.getName());

                if (!p.getId().isEmpty()) {
                    String value = message.getPropertyById(p.getId());
                    channelUID = new ChannelUID(getThing().getUID(), p.getId());
                    updateChannel(channelUID, value);

                    logger.trace("  updateChannel: {} = {}", channelUID, value);
                }
            }

            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(CaddxZoneActions.class);
    }

    public void bypass() {
        String cmd = CaddxBindingConstants.ZONE_BYPASSED;
        String data = String.format("%d", getZoneNumber() - 1);

        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }
        bridgeHandler.sendCommand(CaddxMessageContext.COMMAND, cmd, data);
    }
}
