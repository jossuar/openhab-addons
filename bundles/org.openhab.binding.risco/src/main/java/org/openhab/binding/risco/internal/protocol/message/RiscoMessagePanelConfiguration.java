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
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.protocol.MessageProperty;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoMessagePanelConfiguration extends RiscoMessage {

    private List<MessageProperty> messageProperties = new ArrayList<MessageProperty>();

    public RiscoMessagePanelConfiguration(int commandId, String commandName, String modifier, String[] commandValues,
            int indexFrom, int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public List<MessageProperty> getProperties() {
        if (messageProperties.isEmpty() && commandValues.length > 0) {
            List<MessageProperty> props = new ArrayList<MessageProperty>();

            props.add(new MessageProperty(RiscoBindingConstants.SYSTEM_THING_TYPE, "system", "name", commandValues[0]));
            messageProperties = props;
        }

        return messageProperties;
    }
}
