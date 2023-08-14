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
public class BusExpanderAllocation extends RiscoMessage {
    public static final String COMMAND = "BZEALOC&";

    private List<RiscoThing> messageThings = new ArrayList<RiscoThing>();

    public BusExpanderAllocation(int commandId, String commandName, String modifier, String[] commandValues,
            int indexFrom, int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public List<RiscoThing> getThings() {
        if (messageThings.isEmpty() && commandValues.length > 0) {
            List<RiscoThing> things = new ArrayList<RiscoThing>();

            // BZEALOC&=00000000
            // result: none set
            int index = 1;
            String value = commandValues[0];
            for (int i = 0; i < value.length(); i++) {
                String text = String
                        .format("%4s", Integer.toBinaryString(Integer.parseInt(String.valueOf(value.charAt(i)), 16)))
                        .replaceAll(" ", "0");
                for (int j = text.length() - 1; j >= 0; j--) {
                    if ("1".equals(String.valueOf(text.charAt(j)))) {
                        List<RiscoProperty> props = new ArrayList<RiscoProperty>();
                        props.add(new RiscoProperty("name", "Zone Expander " + index));

                        things.add(new RiscoThing(RiscoThingType.ZONE_EXPANDER, index, props));
                    }
                    index++;
                }
            }

            messageThings = things;
        }

        return messageThings;
    }
}
