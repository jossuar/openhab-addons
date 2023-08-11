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
import org.openhab.core.thing.ThingUID;

/**
 * The {@link DiscoveryInfo} holds the information to add a new discovered item
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class DiscoveryInfo {
    private final ThingUID thingUID;
    private final String thingLabel;
    @Nullable
    private final String indexProperty;
    @Nullable
    private final Integer index;

    public DiscoveryInfo(ThingUID thingUID, String thingLabel, @Nullable String indexProperty,
            @Nullable Integer index) {
        this.thingUID = thingUID;
        this.thingLabel = thingLabel;
        this.indexProperty = indexProperty;
        this.index = index;
    }

    public ThingUID getThingUID() {
        return thingUID;
    }

    public String getThingLabel() {
        return thingLabel;
    }

    @Nullable
    public String getIndexProperty() {
        return indexProperty;
    }

    @Nullable
    public Integer getIndex() {
        return index;
    }
}
