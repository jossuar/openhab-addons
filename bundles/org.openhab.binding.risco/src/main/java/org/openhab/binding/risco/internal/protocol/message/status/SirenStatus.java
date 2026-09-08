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
public class SirenStatus extends RiscoMessage {
    public static final String COMMAND = "SNSTT";

    // @formatter:off
    private final StatusProperty[] properties = {
        new StatusProperty("radio-low-battery-trouble", "R"),
            new StatusProperty("speaker-low-battery-trouble", "S"),
            new StatusProperty("battery-load", "O"),
            new StatusProperty("tamper", "T"),
            new StatusProperty("communication-trouble", "C"),
            new StatusProperty("exists", "E"),
            new StatusProperty("proximity-tamper", "P"),
            new StatusProperty("aux-trouble", "U"),
            new StatusProperty("speaker-flt", "N"),
            new StatusProperty("charge-trouble", "T"),
            new StatusProperty("invalid", "T"),
            new StatusProperty("box-tamper", "T"),
            new StatusProperty("lost", "T"),
            new StatusProperty("low-battery", "T") };
    // @formatter:on

    private List<RiscoThing> messageThings = new ArrayList<RiscoThing>();

    public SirenStatus(int commandId, String commandName, String modifier, String[] commandValues, int indexFrom,
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

                    things.add(new RiscoThing(RiscoThingType.SIREN, index, props));
                }
            }

            messageThings = things;
        }

        return messageThings;
    }

    public static String getReadCommand(int sirenNumber) {
        return String.format(COMMAND + "%d?", sirenNumber);
    }
}
