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
import org.openhab.binding.risco.internal.protocol.StatusProperty;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ZoneStatus extends RiscoMessage {
    public static final String COMMAND = "ZSTT";

    // @formatter:off
    private final StatusProperty[] properties = {
            new StatusProperty("open", "O"),
            new StatusProperty("arm", "A"),
            new StatusProperty("alarm", "a"),
            new StatusProperty("tamper", "T"),
            new StatusProperty("trouble", "R"),
            new StatusProperty("lost", "L"),
            new StatusProperty("low-battery", "B"),
            new StatusProperty("bypass", "Y"),
            new StatusProperty("communication-trouble", "C"),
            new StatusProperty("soak-test", "S"),
            new StatusProperty("hours24", "H"),
            new StatusProperty("not-used", "N"),
            new StatusProperty("exists", "E") };
    // @formatter:on

    private List<RiscoThing> messageThings = new ArrayList<RiscoThing>();

    public ZoneStatus(int commandId, String commandName, String modifier, String[] commandValues, int indexFrom,
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

                    things.add(new RiscoThing(RiscoThingType.ZONE, index, props));
                }
            }

            messageThings = things;
        }

        return messageThings;
    }

    public static String getReadCommand(int zoneNumber) {
        return String.format(COMMAND + "%d?", zoneNumber);
    }
}
