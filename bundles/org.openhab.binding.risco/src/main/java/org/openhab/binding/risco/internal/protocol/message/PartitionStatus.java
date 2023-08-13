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
public class PartitionStatus extends RiscoMessage {
    public static final String COMMAND = "PSTT";

    // @formatter:off
    private final STTProperty[] properties = {
            new STTProperty("duress", "D"),
            new STTProperty("false-code", "C"),
            new STTProperty("fire", "F"),
            new STTProperty("panic", "P"),
            new STTProperty("medic", "M"),
            new STTProperty("arm", "A"),
            new STTProperty("home-stay", "H"),
            new STTProperty("ready-to-arm", "R"),
            new STTProperty("exists", "E"),
            new STTProperty("reset-required", "S"),
            new STTProperty("no-activity-alert", "N"),
            new STTProperty("group-a-arm", "1"),
            new STTProperty("group-b-arm", "2"),
            new STTProperty("group-c-arm", "3"),
            new STTProperty("group-d-arm", "4"),
            new STTProperty("trouble", "T") };
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

                    for (STTProperty prop : properties) {
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
