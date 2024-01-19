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
package org.openhab.binding.bdrthermea.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link BdrThermeaBridgeConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class BdrThermeaBridgeConfiguration {
    public String brand = "baxi";

    public @Nullable String email;
    public @Nullable String password;

    public String getBrand() {
        return brand;
    }

    public @Nullable String getEmail() {
        return email;
    }

    public @Nullable String getPassword() {
        return password;
    }
}
