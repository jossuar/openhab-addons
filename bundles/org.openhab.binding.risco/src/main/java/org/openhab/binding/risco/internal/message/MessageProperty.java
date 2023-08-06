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
import org.openhab.core.thing.ThingTypeUID;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class MessageProperty {
    private final ThingTypeUID thingTypeUID;
    private final String thingUID;
    private final String name;
    private final String value;

    public MessageProperty(ThingTypeUID thingTypeUID, String thingUID, String name, String value) {
        this.thingTypeUID = thingTypeUID;
        this.thingUID = thingUID;
        this.name = name;
        this.value = value;
    }

    public ThingTypeUID getThingTypeUID() {
        return thingTypeUID;
    }

    public String getThingUID() {
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
