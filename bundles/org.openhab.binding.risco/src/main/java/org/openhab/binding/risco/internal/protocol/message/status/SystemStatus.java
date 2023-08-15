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
package org.openhab.binding.risco.internal.protocol.message.status;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoProperty;
import org.openhab.binding.risco.internal.protocol.RiscoThing;
import org.openhab.binding.risco.internal.protocol.RiscoThingType;
import org.openhab.binding.risco.internal.protocol.StatusProperty;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class SystemStatus extends RiscoMessage {
    public static final String COMMAND = "SSTT";

    // @formatter:off
    private final StatusProperty[] properties = {
            new StatusProperty("low-battery-trouble", "B"),
            new StatusProperty("ac-trouble", "A"),
            new StatusProperty("phone-line-trouble", "P"),
            new StatusProperty("clock-trouble", "C"),
            new StatusProperty("default-switch", "D"),
            new StatusProperty("ms-1-report-trouble", "1"),
            new StatusProperty("ms-2-report-trouble", "2"),
            new StatusProperty("ms-3-report-trouble", "3"),
            new StatusProperty("box-tamper", "X"),
            new StatusProperty("jamming-trouble", "J"),
            new StatusProperty("prog-mode", "I"),
            new StatusProperty("learn-mode", "L"),
            new StatusProperty("three-min-bypass", "M"),
            new StatusProperty("walk-test", "W"),
            new StatusProperty("aux-trouble", "U"),
            new StatusProperty("rs485-bus-trouble", "R"),
            new StatusProperty("ls-switch", "S"),
            new StatusProperty("bell-switch", "F"),
            new StatusProperty("bell-trouble", "E"),
            new StatusProperty("bell-tamper", "Y"),
            new StatusProperty("service-expired", "V"),
            new StatusProperty("payment-expired", "T"),
            new StatusProperty("service-mode", "Z"),
            new StatusProperty("dual-path", "Q"),
            new StatusProperty("bus-speed", "H") };
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

                    for (StatusProperty prop : properties) {
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
