/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.mysensors.converter;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.MySensorsBindingConstants;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessageSubType;
import org.openhab.binding.mysensors.internal.sensors.MySensorsVariable;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Converter to adapt state of OpenHab to MySensors and vice versa
 *
 * @author Andrea Cioni - Initial contribution
 *
 */
@NonNullByDefault
public interface MySensorsTypeConverter {

    /**
     * Convert a value from MySensors variable to OH state
     *
     * @param value non-null that should be converted
     * @return the state from a variable
     */
    default State stateFromChannel(MySensorsVariable value) {
        @Nullable
        String actualValue = value.getValue();
        if (actualValue == null) {
            throw new IllegalArgumentException("Cannot get State from null Channel value");
        }
        return fromString(actualValue);
    }

    /**
     * Given a payload string, build an OpenHab state
     *
     * @param string the payload to process
     * @return an equivalent state for OpenHab
     */
    State fromString(String string);

    /**
     * Get a string from an OpenHab command.
     *
     * @param command, the command from OpenHab environment
     * @return the payload string
     */
    default String fromCommand(Command command) {
        return command.toString();
    }

    /**
     * Sometimes payload is not sufficient to build a message for MySensors (see S_COVER: V_UP,V_DOWN,V_STOP).
     * In most cases default implementation is enough.
     *
     * @param channel of the thing that receive an update
     * @param command the command received
     * @return the variable number
     */
    default MySensorsMessageSubType typeFromChannelCommand(String channel, Command command) {
        @Nullable
        MySensorsMessageSubType subType = MySensorsBindingConstants.INVERSE_CHANNEL_MAP.get(channel);
        if (subType != null) {
            return subType;
        } else {
            throw new IllegalArgumentException("Channel not found " + channel);
        }
    }
}
