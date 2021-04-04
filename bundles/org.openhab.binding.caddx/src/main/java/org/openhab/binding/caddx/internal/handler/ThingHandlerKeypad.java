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
package org.openhab.binding.caddx.internal.handler;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.caddx.internal.CaddxBindingConstants;
import org.openhab.binding.caddx.internal.CaddxEvent;
import org.openhab.binding.caddx.internal.CaddxMessage;
import org.openhab.binding.caddx.internal.CaddxMessageType;
import org.openhab.binding.caddx.internal.CaddxProperty;
import org.openhab.binding.caddx.internal.action.CaddxKeypadActions;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusInfo;
import org.openhab.core.thing.binding.ThingHandlerService;
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

    public ThingHandlerKeypad(Thing thing) {
        super(thing, CaddxThingType.KEYPAD);
    }

    @Override
    public void updateChannel(ChannelUID channelUID, String data) {
        if (channelUID.getId().equals(CaddxBindingConstants.KEYPAD_KEY_PRESSED)) {
            StringType stringType = new StringType(data);
            updateState(channelUID, stringType);
        }
    }

    @Override
    public void caddxEventReceived(CaddxEvent event, Thing thing) {
        logger.trace("caddxEventReceived(): Event Received - {}.", event);

        if (getThing().equals(thing)) {
            CaddxMessage message = event.getCaddxMessage();
            CaddxMessageType mt = message.getCaddxMessageType();

            // Log event messages have special handling
            if (CaddxMessageType.KEYPAD_MESSAGE_RECEIVED.equals(mt)) {
                for (CaddxProperty p : mt.properties) {
                    if (!("".equals(p.getId()))) {
                        String value = message.getPropertyById(p.getId());
                        ChannelUID channelUID = new ChannelUID(getThing().getUID(), p.getId());
                        updateChannel(channelUID, value);
                    }
                }
            }

            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        // Keypad follows the status of the bridge
        updateStatus(bridgeStatusInfo.getStatus());

        super.bridgeStatusChanged(bridgeStatusInfo);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(CaddxKeypadActions.class);
    }

    public void enterTerminalMode(String seconds) {
        String cmd = CaddxBindingConstants.KEYPAD_TERMINAL_MODE_REQUEST;
        logger.debug("Address: {}, Seconds: {}", getKeypadAddress(), Integer.parseInt(seconds));
        String data = String.format("%d,%d", getKeypadAddress(), Integer.parseInt(seconds));

        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }
        bridgeHandler.sendCommand(cmd, data);
    }

    public void sendKeypadTextMessage(String line1, String line2) {
        logger.debug("keypad Address: {}, line1: [{}], line2: [{}]", getKeypadAddress(), line1, line2);

        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        String cmd = CaddxBindingConstants.KEYPAD_SEND_KEYPAD_TEXT_MESSAGE;
        String data1 = String.format("%d,0,0,%d,%d,%d,%d,%d,%d,%d,%d", getKeypadAddress(), (int) line1.charAt(0),
                (int) line1.charAt(1), (int) line1.charAt(2), (int) line1.charAt(3), (int) line1.charAt(4),
                (int) line1.charAt(5), (int) line1.charAt(6), (int) line1.charAt(7));
        String data2 = String.format("%d,0,8,%d,%d,%d,%d,%d,%d,%d,%d", getKeypadAddress(), (int) line1.charAt(8),
                (int) line1.charAt(9), (int) line1.charAt(10), (int) line1.charAt(11), (int) line1.charAt(12),
                (int) line1.charAt(13), (int) line1.charAt(14), (int) line1.charAt(15));
        String data3 = String.format("%d,0,16,%d,%d,%d,%d,%d,%d,%d,%d", getKeypadAddress(), (int) line2.charAt(0),
                (int) line2.charAt(1), (int) line2.charAt(2), (int) line2.charAt(3), (int) line2.charAt(4),
                (int) line2.charAt(5), (int) line2.charAt(6), (int) line2.charAt(7));
        String data4 = String.format("%d,0,24,%d,%d,%d,%d,%d,%d,%d,%d", getKeypadAddress(), (int) line2.charAt(8),
                (int) line2.charAt(9), (int) line2.charAt(10), (int) line2.charAt(11), (int) line2.charAt(12),
                (int) line2.charAt(13), (int) line2.charAt(14), (int) line2.charAt(15));

        bridgeHandler.sendCommand(cmd, data1);
        bridgeHandler.sendCommand(cmd, data2);
        bridgeHandler.sendCommand(cmd, data3);
        bridgeHandler.sendCommand(cmd, data4);
    }
}
