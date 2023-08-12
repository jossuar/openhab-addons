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
public class ZoneLabel extends RiscoMessage {
    public static final String COMMAND = "ZLBL";

    private List<RiscoThing> messageThings = new ArrayList<RiscoThing>();

    public ZoneLabel(int commandId, String commandName, String modifier, String[] commandValues, int indexFrom,
            int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public List<RiscoThing> getThings() {
        if (messageThings.isEmpty() && commandValues.length > 0) {
            List<RiscoThing> things = new ArrayList<RiscoThing>();

            for (int index = indexFrom; index <= indexTo; index++) {
                List<RiscoProperty> props = new ArrayList<RiscoProperty>();
                props.add(new RiscoProperty("name", commandValues[index - indexFrom]));
                things.add(new RiscoThing(RiscoThingType.ZONE, index, props));
            }

            messageThings = things;
        }

        return messageThings;
    }

    public static String getReadCommand(int zoneNumber) {
        return String.format(COMMAND + "%d?", zoneNumber);
    }
}
