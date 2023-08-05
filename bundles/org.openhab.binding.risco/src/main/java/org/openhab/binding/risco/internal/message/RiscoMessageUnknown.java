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
package org.openhab.binding.risco.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoMessageUnknown extends RiscoMessage {

    public RiscoMessageUnknown(int commandId, String commandName, String modifier, String[] commandValues,
            int indexFrom, int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public ThingProperty[] getProperties() {
        // Return an empty property array
        return new ThingProperty[0];
    }
}
