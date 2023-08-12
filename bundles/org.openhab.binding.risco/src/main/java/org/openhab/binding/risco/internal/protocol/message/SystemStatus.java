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
package org.openhab.binding.risco.internal.protocol.message;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoProperty;
import org.openhab.binding.risco.internal.protocol.RiscoThing;
import org.openhab.binding.risco.internal.protocol.RiscoThingType;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class SystemStatus extends RiscoMessage {
    public static final String COMMAND = "SSTT";

    // @formatter:off
    private final STTProperty[] properties = {
            new STTProperty("low_battery_trouble", "B"),
            new STTProperty("ac_trouble", "A"),
            new STTProperty("phone_line_trouble", "P"),
            new STTProperty("clock_trouble", "C"),
            new STTProperty("default_switch", "D"),
            new STTProperty("ms1_report_trouble", "1"),
            new STTProperty("ms2_report_trouble", "2"),
            new STTProperty("ms3_report_trouble", "3"),
            new STTProperty("box_tamper", "X"),
            new STTProperty("jamming_trouble", "J"),
            new STTProperty("prog_mode", "I"),
            new STTProperty("learn_mode", "L"),
            new STTProperty("three_min_bypass", "M"),
            new STTProperty("walk_test", "W"),
            new STTProperty("aux_trouble", "U"),
            new STTProperty("rs485_bus_trouble", "R"),
            new STTProperty("ls_switch", "S"),
            new STTProperty("bell_switch", "F"),
            new STTProperty("bell_trouble", "E"),
            new STTProperty("bell_tamper", "Y"),
            new STTProperty("service_expired", "V"),
            new STTProperty("payment_expired", "T"),
            new STTProperty("service_mode", "Z"),
            new STTProperty("dual_path", "Q"),
            new STTProperty("bus_speed", "H") };
    // @formatter:on

    private List<RiscoThing> messageThings = new ArrayList<RiscoThing>();

    public SystemStatus(int commandId, String commandName, String modifier, String[] commandValues, int indexFrom,
            int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public List<RiscoThing> getThings() {
        if (isWriteMessage() && messageThings.isEmpty()) {
            List<RiscoThing> things = new ArrayList<RiscoThing>();

            // loop from indexFrom to indexTo
            for (int index = indexFrom; index <= indexTo; index++) {
                String value = commandValues[index - indexFrom];

                if (value != null) {
                    List<RiscoProperty> props = new ArrayList<RiscoProperty>();

                    for (STTProperty prop : properties) {
                        if (value.contains(prop.flag)) {
                            props.add(new RiscoProperty(prop.property, "true"));
                        } else {
                            props.add(new RiscoProperty(prop.property, "false"));
                        }
                    }

                    things.add(new RiscoThing(RiscoThingType.SYSTEM, index, props));
                }
            }

            messageThings = things;
        }

        return messageThings;
    }

    public static String getReadCommand() {
        return String.format(COMMAND + "?");
    }
}
