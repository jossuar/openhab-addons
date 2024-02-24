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
package org.openhab.binding.risco.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration class for the Risco Zone Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoZoneExpanderConfiguration {

    // Zone Thing constants
    public static final String ZONE_EXPANDER_NUMBER = "zoneExpanderNumber";

    /**
     * The Zone Expander Number. Can be in the range of 1-64. Depends on the Panel model. This is a required parameter
     * for a zone expander.
     */
    private int zoneExpanderNumber;

    public int getZoneExpanderNumber() {
        return zoneExpanderNumber;
    }
}
