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
package org.openhab.binding.caddx.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration class for the Caddx Zone Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class CaddxZoneConfiguration {

    // Zone Thing constants
    public static final String ZONE_NUMBER = "zoneNumber";
    public static final String IGNORE_ZONE_STATUS_TRANSITIONS = "ignoreZoneStatusTransitions";

    /**
     * The Zone Number. Can be in the range of 1-192. Depends on the Panel model. This is a required parameter for a
     * zone.
     */
    private int zoneNumber;
    private boolean ignoreZoneStatusTransitions;

    public int getZoneNumber() {
        return zoneNumber;
    }

    public boolean getIgnoreZoneStatusTransitions() {
        return ignoreZoneStatusTransitions;
    }
}
