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
public class VoiceModuleStatus extends RiscoMessage {
    public static final String COMMAND = "VMSTT";

    // @formatter:off
    private final StatusProperty[] properties = {
            new StatusProperty("tamper", "T"),
            new StatusProperty("communication-trouble", "C"),
            new StatusProperty("exists", "E") };
    // @formatter:on

    private List<RiscoThing> messageThings = new ArrayList<RiscoThing>();

    public VoiceModuleStatus(int commandId, String commandName, String modifier, String[] commandValues, int indexFrom,
            int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public List<RiscoThing> getThings() {
        if (isWriteMessage() && messageThings.isEmpty() && commandValues.length > 0) {
            List<RiscoThing> things = new ArrayList<RiscoThing>();

            String value = commandValues[0];

            if (value != null) {
                List<RiscoProperty> props = new ArrayList<RiscoProperty>();

                for (StatusProperty prop : properties) {
                    if (value.contains(prop.flag)) {
                        props.add(new RiscoProperty(prop.property, "true"));
                    } else {
                        props.add(new RiscoProperty(prop.property, "false"));
                    }
                }

                things.add(new RiscoThing(RiscoThingType.VOICE_MODULE, null, props));
            }

            messageThings = things;
        }

        return messageThings;
    }

    public static String getReadCommand() {
        return String.format(COMMAND + "?");
    }
}
