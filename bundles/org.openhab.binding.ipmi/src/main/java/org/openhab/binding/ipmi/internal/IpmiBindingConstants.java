/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.ipmi.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link IpmiBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class IpmiBindingConstants {
    // Binding ID
    private static final String BINDING_ID = "ipmi";

    // List of bridge device types
    public static final String IPMI_BRIDGE = "bridge";

    // List of all Bridge Thing Type UIDs
    public static final ThingTypeUID IPMIBRIDGE_THING_TYPE = new ThingTypeUID(BINDING_ID, IPMI_BRIDGE);

    // Set of all supported Thing Type UIDs
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(IPMIBRIDGE_THING_TYPE).collect(Collectors.toSet()));

    // Set of all supported Bridge Type UIDs
    public static final Set<ThingTypeUID> SUPPORTED_BRIDGE_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(IPMIBRIDGE_THING_TYPE).collect(Collectors.toSet()));
}
