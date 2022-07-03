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
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StopMoveType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;

/**
 * Used to convert a String from an incoming MySensors message to a UpDownType
 *
 * @author Tim Oberföll - Initial contribution
 * @author Andrea Cioni - Redesign
 *
 */
@NonNullByDefault
public class MySensorsUpDownTypeConverter implements MySensorsTypeConverter {

    @Override
    public MySensorsMessageSubType typeFromChannelCommand(String channel, Command command) {
        if (channel.equals(MySensorsBindingConstants.CHANNEL_COVER)) {
            if (command instanceof UpDownType) {
                if (command == UpDownType.UP) {
                    return MySensorsMessageSubType.V_UP;
                } else if (command == UpDownType.DOWN) {
                    return MySensorsMessageSubType.V_DOWN;
                }
            } else if (command instanceof StopMoveType) {
                if (command == StopMoveType.STOP) {
                    return MySensorsMessageSubType.V_STOP;
                }
            } else if (command instanceof PercentType) {
                return MySensorsMessageSubType.V_PERCENTAGE;
            }
        }
        throw new IllegalArgumentException("Invalid command (" + command + ") passed to UpDown adapter");
    }

    @Override
    public State stateFromChannel(MySensorsVariable value) {
        if (value.getType() == MySensorsMessageSubType.V_DOWN) {
            return UpDownType.DOWN;
        } else if (value.getType() == MySensorsMessageSubType.V_UP) {
            return UpDownType.UP;
        } else if (value.getType() == MySensorsMessageSubType.V_PERCENTAGE) {
            @Nullable
            String actualValue = value.getValue();
            if (actualValue != null) {
                return new PercentType(actualValue);
            } else {
                throw new IllegalArgumentException("Cannot set stateFromChannel because V_PERCENTAGE has null value");
            }
        } else {
            throw new IllegalArgumentException("Variable " + value.getType() + " is not up/down or percent type");
        }
    }

    @Override
    public String fromCommand(Command state) {
        if (state instanceof UpDownType) {
            if (state == UpDownType.DOWN || state == UpDownType.UP) {
                return "1";
            } else {
                throw new IllegalStateException("Invalid UpDown state: " + state);
            }
        } else if (state instanceof StopMoveType) {
            return "1";
        } else if (state instanceof PercentType) {
            return state.toFullString();
        } else {
            throw new IllegalStateException(
                    "UpDown/Percent command are the only one command allowed by this adapter, passed: " + state + "("
                            + (state.getClass()) + ")");
        }
    }

    @Override
    public State fromString(String string) {
        throw new IllegalStateException(
                "UpDown type state could not determinateted from a string, use stateFromChannel");
    }
}
