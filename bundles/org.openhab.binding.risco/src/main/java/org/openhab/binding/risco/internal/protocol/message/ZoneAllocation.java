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
import org.openhab.binding.risco.internal.protocol.MessageProperty;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoThingType;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ZoneAllocation extends RiscoMessage {
    public static final String COMMAND1 = "ZALOC&";
    public static final String COMMAND2 = "WZALOC&";
    public static final String COMMAND3 = "BZALOC&";

    private List<MessageProperty> messageProperties = new ArrayList<MessageProperty>();

    public ZoneAllocation(int commandId, String commandName, String modifier, String[] commandValues, int indexFrom,
            int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public List<MessageProperty> getProperties() {
        if (messageProperties.isEmpty() && commandValues.length > 0) {
            List<MessageProperty> props = new ArrayList<MessageProperty>();

            // ZALOC&=FFFFFFF1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000
            // result: 1-29 set
            int num2 = 1;
            String value = commandValues[0];
            for (int i = 0; i < value.length(); i++) {
                String text = String
                        .format("%4s", Integer.toBinaryString(Integer.parseInt(String.valueOf(value.charAt(i)), 16)))
                        .replaceAll(" ", "0");
                for (int j = text.length() - 1; j >= 0; j--) {
                    if ("1".equals(String.valueOf(text.charAt(j)))) {
                        props.add(
                                new MessageProperty(RiscoThingType.ZONE, num2, "name", String.format("Zone %d", num2)));
                    }
                    num2++;
                }
            }

            messageProperties = props;
        }

        return messageProperties;
    }
}
