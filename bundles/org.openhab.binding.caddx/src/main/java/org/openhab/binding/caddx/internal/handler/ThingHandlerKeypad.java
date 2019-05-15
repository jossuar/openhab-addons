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
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.caddx.internal.CaddxBindingConstants;
import org.openhab.binding.caddx.internal.CaddxEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a class for handling a Keypad type Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ThingHandlerKeypad extends CaddxBaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ThingHandlerKeypad.class);

    /**
     * Constructor.
     *
     * @param thing
     */
    public ThingHandlerKeypad(Thing thing) {
        super(thing, CaddxThingType.KEYPAD);
    }

    @Override
    public void updateChannel(ChannelUID channelUID, String data) {
        logger.debug("updateChannel(): Keypad Channel UID: {}", channelUID);
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
            return;
        } else if (channelUID.getId().equals(CaddxBindingConstants.PARTITION_PRIMARY_COMMAND)) {
            cmd = channelUID.getId();
            data = command.toString() + "," + String.format("%d", getPartitionNumber() - 1);
        } else {
            throw new IllegalArgumentException("Unknown command");
        }

        if (!data.startsWith("-")) {
            bridgeHandler.sendCommand(cmd, data);
        }
    }

    @Override
    public void caddxEventReceived(CaddxEvent event, Thing thing) {
    }
}
