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
package org.openhab.binding.risco.internal.protocol;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class MessageProperty {
    private final RiscoThingType type;
    @Nullable
    private final Integer index;
    private final String name;
    private final String value;

    public MessageProperty(RiscoThingType type, @Nullable Integer index, String name, String value) {
        this.type = type;
        this.index = index;
        this.name = name;
        this.value = value;
    }

    public RiscoThingType getRiscoThingType() {
        return type;
    }

    @Nullable
    public Integer getIndex() {
        return index;
    }

    public String getKey() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append(type.toString()).append(index).append(" - ").append(name).append(": ").append(value);
        return sb.toString();
    }
}
