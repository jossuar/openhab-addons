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
package org.openhab.binding.risco.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Configuration class for the Risco Output Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoOutputConfiguration {

    // Zone Thing constants
    public static final String OUTPUT_NUMBER = "outputNumber";

    /**
     * The Output Number. Can be in the range of 1-196. Depends on the Panel model. This is a required parameter for a
     * output.
     */
    private int outputNumber;

    public int getOutputNumber() {
        return outputNumber;
    }
}
