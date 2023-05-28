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
package org.openhab.binding.mysensors.internal.sensors;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.internal.exception.RevertVariableStateException;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessage;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessageSubType;

/**
 * Variables (states) of a MySensors child.
 *
 * @author Tim Oberföll - Initial contribution
 * @author Andrea Cioni - Redesign
 *
 */
@NonNullByDefault
public abstract class MySensorsVariable {

    private final MySensorsMessageSubType type;

    @Nullable
    private String value;

    @Nullable
    private Date lastUpdate;

    @Nullable
    private String oldState;

    @Nullable
    private Date oldLastUpdate;

    public MySensorsVariable(MySensorsMessageSubType type) {
        this.type = type;
    }

    @Nullable
    public synchronized String getValue() {
        return value;
    }

    public synchronized void setValue(@Nullable String value) {
        oldState = getValue();
        oldLastUpdate = getLastUpdate();
        setLastUpdate(new Date());
        this.value = value;
    }

    public synchronized void setValue(MySensorsMessage message) {
        setValue(message.getMsg());
    }

    public synchronized MySensorsMessageSubType getType() {
        return type;
    }

    @Nullable
    public synchronized Date getLastUpdate() {
        return lastUpdate;
    }

    public synchronized void setLastUpdate(@Nullable Date lastupdate) {
        this.lastUpdate = lastupdate;
    }

    public synchronized boolean isRevertible() {
        return (oldState != null && oldLastUpdate != null);
    }

    public synchronized void revertValue() throws RevertVariableStateException {
        if (isRevertible()) {
            setValue(oldState);
            setLastUpdate(oldLastUpdate);
            oldState = null;
            oldLastUpdate = null;
        } else {
            throw new RevertVariableStateException();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + type.getId();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MySensorsVariable other = (MySensorsVariable) obj;
        if (type != other.type) {
            return false;
        }
        if (value == null) {
            return other.value == null;
        } else
            return value.equals(other.value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [value=" + value + "]";
    }
}
