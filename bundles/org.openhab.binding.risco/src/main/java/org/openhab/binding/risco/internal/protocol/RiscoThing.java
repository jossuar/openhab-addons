/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoThing {
    private final RiscoThingType type;
    @Nullable
    private final Integer index;
    private List<RiscoProperty> properties;

    public RiscoThing(RiscoThingType type, @Nullable Integer index, List<RiscoProperty> properties) {
        this.type = type;
        this.index = index;
        this.properties = properties;
    }

    public RiscoThingType getRiscoThingType() {
        return type;
    }

    @Nullable
    public Integer getIndex() {
        return index;
    }

    public List<RiscoProperty> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append(type.toString()).append(index).append(" - ").append(properties);
        return sb.toString();
    }
}
