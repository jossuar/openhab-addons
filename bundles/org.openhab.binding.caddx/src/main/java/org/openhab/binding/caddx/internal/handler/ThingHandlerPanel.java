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
package org.openhab.binding.caddx.internal.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.caddx.internal.CaddxBindingConstants;
import org.openhab.binding.caddx.internal.CaddxEvent;
import org.openhab.binding.caddx.internal.CaddxMessage;
import org.openhab.binding.caddx.internal.CaddxMessage.CaddxMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a class for handling a Panel type Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ThingHandlerPanel extends CaddxBaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ThingHandlerPanel.class);

    /**
     * Constructor.
     *
     * @param thing
     */
    public ThingHandlerPanel(Thing thing) {
        super(thing, CaddxThingType.PANEL);
    }

    @Override
    public void updateChannel(ChannelUID channelUID, String data) {
        logger.debug("updateChannel(): Panel Channel UID: {}", channelUID);

        if (channelUID.getId().equals(CaddxBindingConstants.PANEL_FIRMWARE_VERSION)) {
            updateState(channelUID, new StringType(data));
        } else {
            // All Panel channels are OnOffType
            OnOffType onOffType;

            onOffType = ("true".equals(data)) ? OnOffType.ON : OnOffType.OFF;
            updateState(channelUID, onOffType);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        String cmd = null;
        String data = null;
        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        if (command instanceof RefreshType) {
            if (channelUID.getId().equals(CaddxBindingConstants.PANEL_FIRMWARE_VERSION)) {
                cmd = CaddxBindingConstants.PANEL_INTERFACE_CONFIGURATION_REQUEST;
                data = "";
            } else {
                return;
            }
        } else {
            throw new IllegalArgumentException("Unknown command");
        }

        bridgeHandler.sendCommand(cmd, data);
    }

    @Override
    public void caddxEventReceived(CaddxEvent event, Thing thing) {
        logger.debug("caddxEventReceived(): Event Received - {} {}.", event);

        if (getThing().equals(thing)) {
            CaddxMessage message = event.getCaddxMessage();
            CaddxMessageType mt = message.getCaddxMessageType();
            ChannelUID channelUID = null;

            for (CaddxMessage.Property p : mt.properties) {
                if (!("".equals(p.getId()))) {
                    String value = message.getPropertyById(p.getId());
                    channelUID = new ChannelUID(getThing().getUID(), p.getId());
                    updateChannel(channelUID, value);
                }
            }

            updateStatus(ThingStatus.ONLINE);
        }
    }
}
