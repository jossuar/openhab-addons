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
            new STTProperty("low-battery-trouble", "B"),
            new STTProperty("ac-trouble", "A"),
            new STTProperty("phone-line-trouble", "P"),
            new STTProperty("clock-trouble", "C"),
            new STTProperty("default-switch", "D"),
            new STTProperty("ms-1-report-trouble", "1"),
            new STTProperty("ms-2-report-trouble", "2"),
            new STTProperty("ms-3-report-trouble", "3"),
            new STTProperty("box-tamper", "X"),
            new STTProperty("jamming-trouble", "J"),
            new STTProperty("prog-mode", "I"),
            new STTProperty("learn-mode", "L"),
            new STTProperty("three-min-bypass", "M"),
            new STTProperty("walk-test", "W"),
            new STTProperty("aux-trouble", "U"),
            new STTProperty("rs485-bus-trouble", "R"),
            new STTProperty("ls-switch", "S"),
            new STTProperty("bell-switch", "F"),
            new STTProperty("bell-trouble", "E"),
            new STTProperty("bell-tamper", "Y"),
            new STTProperty("service-expired", "V"),
            new STTProperty("payment-expired", "T"),
            new STTProperty("service-mode", "Z"),
            new STTProperty("dual-path", "Q"),
            new STTProperty("bus-speed", "H") };
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
