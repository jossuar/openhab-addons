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
 * Configuration class for the Risco Keyfob Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoKeyfobConfiguration {

    // Zone Thing constants
    public static final String KEYFOB_NUMBER = "keyfobNumber";

    /**
     * The Keyfob Number. Can be in the range of 1-256. Depends on the Panel model. This is a required parameter for a
     * zone.
     */
    private int keyfobNumber;

    public int getKeyfobNumber() {
        return keyfobNumber;
    }
}
