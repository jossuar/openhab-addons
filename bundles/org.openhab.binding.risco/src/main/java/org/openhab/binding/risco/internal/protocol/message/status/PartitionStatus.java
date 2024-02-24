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
public class PartitionStatus extends RiscoMessage {
    public static final String COMMAND = "PSTT";

    // @formatter:off
    private final StatusProperty[] properties = {
            new StatusProperty("alarm", "a"),
            new StatusProperty("duress", "D"),
            new StatusProperty("false-code", "C"),
            new StatusProperty("fire", "F"),
            new StatusProperty("panic", "P"),
            new StatusProperty("medic", "M"),
            new StatusProperty("arm", "A"),
            new StatusProperty("home-stay", "H"),
            new StatusProperty("ready-to-arm", "R"),
            new StatusProperty("open", "O"),
            new StatusProperty("exists", "E"),
            new StatusProperty("reset-required", "S"),
            new StatusProperty("no-activity-alert", "N"),
            new StatusProperty("group-a-arm", "1"),
            new StatusProperty("group-b-arm", "2"),
            new StatusProperty("group-c-arm", "3"),
            new StatusProperty("group-d-arm", "4"),
            new StatusProperty("trouble", "T") };
    // @formatter:on

    private List<RiscoThing> messageThings = new ArrayList<RiscoThing>();

    public PartitionStatus(int commandId, String commandName, String modifier, String[] commandValues, int indexFrom,
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

                    things.add(new RiscoThing(RiscoThingType.PARTITION, index, props));
                }
            }

            messageThings = things;
        }

        return messageThings;
    }

    public static String getReadCommand(int partitionNumber) {
        return String.format(COMMAND + "%d?", partitionNumber);
    }
}
