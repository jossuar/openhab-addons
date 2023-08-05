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
import org.openhab.core.thing.ThingUID;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ThingProperty {
    private final ThingUID thingUID;
    private final String name;
    private final String value;

    public ThingProperty(ThingUID thingUID, String name, String value) {
        this.thingUID = thingUID;
        this.name = name;
        this.value = value;
    }

    public ThingUID getThingUID() {
        return thingUID;
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

        sb.append("{").append(thingUID.toString()).append(" - ").append(name).append(": ").append(value);
        return sb.toString();
    }
}
