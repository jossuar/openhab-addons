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
 * This is a class for handling a Zone type Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ThingHandlerZone extends CaddxBaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ThingHandlerZone.class);

    /**
     * Constructor.
     *
     * @param thing
     */
    public ThingHandlerZone(Thing thing) {
        super(thing, CaddxThingType.ZONE);
    }

    @Override
    public void updateChannel(ChannelUID channelUID, String data) {
        // logger.trace("updateChannel(): Zone Channel UID: {}", channelUID);

        if (channelUID.getId().equals(CaddxBindingConstants.ZONE_NAME)) {
            /*
             * public static byte ConvertGreekCharToByte(char c)
             * {
             * byte b;
             *
             * switch (c)
             * {
             * case 'Α': b = 0x41; break;
             * case 'Β': b = 0x42; break;
             * case 'Γ': b = 0xB7; break;
             * case 'Δ': b = 0x10; break;
             * case 'Ε': b = 0x45; break;
             * case 'Ζ': b = 0x5A; break;
             * case 'Η': b = 0x48; break;
             * case 'Θ': b = 0x13; break;
             * case 'Ι': b = 0x49; break;
             * case 'Κ': b = 0x4B; break;
             * case 'Λ': b = 0x14; break;
             * case 'Μ': b = 0x4D; break;
             * case 'Ν': b = 0x4E; break;
             * case 'Ξ': b = 0x12; break;
             * case 'Ο': b = 0x4F; break;
             * case 'Π': b = 0xC8; break;
             * case 'Ρ': b = 0x50; break;
             * case 'Σ': b = 0x16; break;
             * case 'Τ': b = 0x54; break;
             * case 'Υ': b = 0x59; break;
             * case 'Φ': b = 0xCC; break;
             * case 'Χ': b = 0x58; break;
             * case 'Ψ': b = 0x17; break;
             * case 'Ω': b = 0x15; break;
             * default:
             * b = Convert.ToByte(c);
             * break;
             * }
             *
             * return b;
             * }
             * public static string ConvertByteToGreekChar(byte b)
             * {
             * string s;
             *
             * switch (b)
             * {
             * case 0xb7: s = "Γ"; break;
             * case 0x10: s = "Δ"; break;
             * case 0x13: s = "Θ"; break;
             * case 0x14: s = "Λ"; break;
             * case 0x12: s = "Ξ"; break;
             * case 0xc8: s = "Π"; break;
             * case 0x16: s = "Σ"; break;
             * case 0xcc: s = "Φ"; break;
             * case 0x17: s = "Ψ"; break;
             * case 0x15: s = "Ω"; break;
             * default:
             * s = Convert.ToChar(b).ToString();
             * break;
             * }
             *
             * return s;
             * }
             *
             */
            getThing().setLabel(data);
            updateState(channelUID, new StringType(data));
        } else {
            // All Zone channels are OnOffType
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
            if (channelUID.getId().equals(CaddxBindingConstants.ZONE_FAULTED)) {
                cmd = CaddxBindingConstants.ZONE_STATUS_REQUEST;
                data = String.format("%d", getZoneNumber() - 1);
            } else if (channelUID.getId().equals(CaddxBindingConstants.ZONE_NAME)) {
                cmd = CaddxBindingConstants.ZONE_NAME_REQUEST;
                data = String.format("%d", getZoneNumber() - 1);
            } else {
                return;
            }
        } else if (channelUID.getId().equals(CaddxBindingConstants.ZONE_BYPASS_TOGGLE)) {
            cmd = channelUID.getId();
            data = String.format("%d", getZoneNumber() - 1);
        } else {
            throw new IllegalArgumentException("Unknown command");
        }

        bridgeHandler.sendCommand(cmd, data);
    }

    @Override
    public void caddxEventReceived(CaddxEvent event, Thing thing) {
        if (logger.isTraceEnabled()) {
            logger.trace("caddxEventReceived(): Event Received - {} {}.", event);
        }

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
