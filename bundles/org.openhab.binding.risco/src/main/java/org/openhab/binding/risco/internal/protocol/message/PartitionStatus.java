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
            new STTProperty("open", "O"),
            new STTProperty("arm", "A"),
            new STTProperty("alarm", "a"),
            new STTProperty("tamper", "T"),
            new STTProperty("trouble", "R"),
            new STTProperty("lost", "L"),
            new STTProperty("low_battery", "B"),
            new STTProperty("bypass", "Y"),
            new STTProperty("comm_trouble", "C"),
            new STTProperty("soak_test", "S"),
            new STTProperty("hours24", "H"),
            new STTProperty("not_used", "N"),
            new STTProperty("exists", "E") };
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
