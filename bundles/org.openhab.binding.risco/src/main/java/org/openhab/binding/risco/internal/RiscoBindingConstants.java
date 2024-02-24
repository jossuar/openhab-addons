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
package org.openhab.binding.risco.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link RiscoBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoBindingConstants {

    // Binding ID
    private static final String BINDING_ID = "risco";

    // List of bridge device types
    public static final String BRIDGE = "bridge";
    // List of device types
    public static final String BUS_EXPANDER = "bus-expander";
    public static final String CELLULAR_ON_BUS = "cellular-on-bus";
    public static final String KEYFOB = "keyfob";
    public static final String KEYPAD = "keypad";
    public static final String OUTPUT = "output";
    public static final String OUTPUT_EXPANDER = "output-expander";
    public static final String PARTITION = "partition";
    public static final String POWER_SUPPLY = "power-supply";
    public static final String PROXIMITY_READER = "proximity-reader";
    public static final String SIREN = "siren";
    public static final String SYSTEM = "system";
    public static final String VOICE_MODULE = "voice-module";
    public static final String WIRELESS_MODULE = "wireless-module";
    public static final String ZONE_EXPANDER = "zone-expander";
    public static final String ZONE = "zone";

    // List of all Bridge Thing Type UIDs
    public static final ThingTypeUID BRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, BRIDGE);

    // List of all Thing Type UIDs
    public static final ThingTypeUID BUS_EXPANDER_THING_TYPE = new ThingTypeUID(BINDING_ID, BUS_EXPANDER);
    public static final ThingTypeUID CELLULAR_ON_BUS_THING_TYPE = new ThingTypeUID(BINDING_ID, CELLULAR_ON_BUS);
    public static final ThingTypeUID KEYFOB_THING_TYPE = new ThingTypeUID(BINDING_ID, KEYFOB);
    public static final ThingTypeUID KEYPAD_THING_TYPE = new ThingTypeUID(BINDING_ID, KEYPAD);
    public static final ThingTypeUID OUTPUT_THING_TYPE = new ThingTypeUID(BINDING_ID, OUTPUT);
    public static final ThingTypeUID OUTPUT_EXPANDER_THING_TYPE = new ThingTypeUID(BINDING_ID, OUTPUT_EXPANDER);
    public static final ThingTypeUID PARTITION_THING_TYPE = new ThingTypeUID(BINDING_ID, PARTITION);
    public static final ThingTypeUID POWER_SUPPLY_THING_TYPE = new ThingTypeUID(BINDING_ID, POWER_SUPPLY);
    public static final ThingTypeUID PROXIMITY_READER_THING_TYPE = new ThingTypeUID(BINDING_ID, PROXIMITY_READER);
    public static final ThingTypeUID SOUNDER_THING_TYPE = new ThingTypeUID(BINDING_ID, SIREN);
    public static final ThingTypeUID SYSTEM_THING_TYPE = new ThingTypeUID(BINDING_ID, SYSTEM);
    public static final ThingTypeUID VOICE_MODULE_THING_TYPE = new ThingTypeUID(BINDING_ID, VOICE_MODULE);
    public static final ThingTypeUID WIRELESS_MODULE_THING_TYPE = new ThingTypeUID(BINDING_ID, WIRELESS_MODULE);
    public static final ThingTypeUID ZONE_THING_TYPE = new ThingTypeUID(BINDING_ID, ZONE);
    public static final ThingTypeUID ZONE_EXPANDER_THING_TYPE = new ThingTypeUID(BINDING_ID, ZONE_EXPANDER);

    // Set of all supported Thing Type UIDs
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableSet(Stream
            .of(BRIDGE_THING_TYPE, BUS_EXPANDER_THING_TYPE, CELLULAR_ON_BUS_THING_TYPE, KEYFOB_THING_TYPE,
                    KEYPAD_THING_TYPE, OUTPUT_THING_TYPE, OUTPUT_EXPANDER_THING_TYPE, PARTITION_THING_TYPE,
                    POWER_SUPPLY_THING_TYPE, PROXIMITY_READER_THING_TYPE, SOUNDER_THING_TYPE, SYSTEM_THING_TYPE,
                    VOICE_MODULE_THING_TYPE, WIRELESS_MODULE_THING_TYPE, ZONE_THING_TYPE, ZONE_EXPANDER_THING_TYPE)
            .collect(Collectors.toSet()));

    // Special channels
    public static final String ZONE_CHANNEL_BYPASS = "bypass";
    public static final String PARTITION_CHANNEL_ARM = "arm";
    public static final String PARTITION_CHANNEL_HOME_STAY = "home-stay";

    // Channel types
    @SuppressWarnings("null")
    public static final Set<String> CHANNEL_TYPES_SWITCH = Collections
            .unmodifiableSet(Stream.of("switch", "readonly-switch").collect(Collectors.toSet()));
    @SuppressWarnings("null")
    public static final Set<String> CHANNEL_TYPES_CONTACT = Collections
            .unmodifiableSet(Stream.of("contact", "readonly-contact").collect(Collectors.toSet()));
    @SuppressWarnings("null")
    public static final Set<String> CHANNEL_TYPES_TEXT = Collections
            .unmodifiableSet(Stream.of("text", "readonly-text").collect(Collectors.toSet()));

    public static final int[] CRC_ARRAY = { 0x0000, 0xc0c1, 0xc181, 0x0140, 0xc301, 0x03c0, 0x0280, 0xc241, 0xc601,
            0x06c0, 0x0780, 0xc741, 0x0500, 0xc5c1, 0xc481, 0x0440, 0xcc01, 0x0cc0, 0x0d80, 0xcd41, 0x0f00, 0xcfc1,
            0xce81, 0x0e40, 0x0a00, 0xcac1, 0xcb81, 0x0b40, 0xc901, 0x09c0, 0x0880, 0xc841, 0xd801, 0x18c0, 0x1980,
            0xd941, 0x1b00, 0xdbc1, 0xda81, 0x1a40, 0x1e00, 0xdec1, 0xdf81, 0x1f40, 0xdd01, 0x1dc0, 0x1c80, 0xdc41,
            0x1400, 0xd4c1, 0xd581, 0x1540, 0xd701, 0x17c0, 0x1680, 0xd641, 0xd201, 0x12c0, 0x1380, 0xd341, 0x1100,
            0xd1c1, 0xd081, 0x1040, 0xf001, 0x30c0, 0x3180, 0xf141, 0x3300, 0xf3c1, 0xf281, 0x3240, 0x3600, 0xf6c1,
            0xf781, 0x3740, 0xf501, 0x35c0, 0x3480, 0xf441, 0x3c00, 0xfcc1, 0xfd81, 0x3d40, 0xff01, 0x3fc0, 0x3e80,
            0xfe41, 0xfa01, 0x3ac0, 0x3b80, 0xfb41, 0x3900, 0xf9c1, 0xf881, 0x3840, 0x2800, 0xe8c1, 0xe981, 0x2940,
            0xeb01, 0x2bc0, 0x2a80, 0xea41, 0xee01, 0x2ec0, 0x2f80, 0xef41, 0x2d00, 0xedc1, 0xec81, 0x2c40, 0xe401,
            0x24c0, 0x2580, 0xe541, 0x2700, 0xe7c1, 0xe681, 0x2640, 0x2200, 0xe2c1, 0xe381, 0x2340, 0xe101, 0x21c0,
            0x2080, 0xe041, 0xa001, 0x60c0, 0x6180, 0xa141, 0x6300, 0xa3c1, 0xa281, 0x6240, 0x6600, 0xa6c1, 0xa781,
            0x6740, 0xa501, 0x65c0, 0x6480, 0xa441, 0x6c00, 0xacc1, 0xad81, 0x6d40, 0xaf01, 0x6fc0, 0x6e80, 0xae41,
            0xaa01, 0x6ac0, 0x6b80, 0xab41, 0x6900, 0xa9c1, 0xa881, 0x6840, 0x7800, 0xb8c1, 0xb981, 0x7940, 0xbb01,
            0x7bc0, 0x7a80, 0xba41, 0xbe01, 0x7ec0, 0x7f80, 0xbf41, 0x7d00, 0xbdc1, 0xbc81, 0x7c40, 0xb401, 0x74c0,
            0x7580, 0xb541, 0x7700, 0xb7c1, 0xb681, 0x7640, 0x7200, 0xb2c1, 0xb381, 0x7340, 0xb101, 0x71c0, 0x7080,
            0xb041, 0x5000, 0x90c1, 0x9181, 0x5140, 0x9301, 0x53c0, 0x5280, 0x9241, 0x9601, 0x56c0, 0x5780, 0x9741,
            0x5500, 0x95c1, 0x9481, 0x5440, 0x9c01, 0x5cc0, 0x5d80, 0x9d41, 0x5f00, 0x9fc1, 0x9e81, 0x5e40, 0x5a00,
            0x9ac1, 0x9b81, 0x5b40, 0x9901, 0x59c0, 0x5880, 0x9841, 0x8801, 0x48c0, 0x4980, 0x8941, 0x4b00, 0x8bc1,
            0x8a81, 0x4a40, 0x4e00, 0x8ec1, 0x8f81, 0x4f40, 0x8d01, 0x4dc0, 0x4c80, 0x8c41, 0x4400, 0x84c1, 0x8581,
            0x4540, 0x8701, 0x47c0, 0x4680, 0x8641, 0x8201, 0x42c0, 0x4380, 0x8341, 0x4100, 0x81c1, 0x8081, 0x4040 };
}
