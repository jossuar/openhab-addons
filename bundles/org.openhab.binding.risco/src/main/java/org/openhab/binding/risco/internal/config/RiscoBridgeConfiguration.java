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
import org.eclipse.jdt.annotation.Nullable;

/**
 * Configuration class for the Risco LightSys panel.
 *
 * @author Georgios Moutsos - Initial contribution
 */

@NonNullByDefault
public class RiscoBridgeConfiguration {

    // Risco Bridge Thing constants
    // public static final String PANEL_IP = "hostname";
    // public static final String PANEL_PORT = "port";
    // public static final String PANEL_ID = "id";
    // public static final String PANEL_PASSWORD = "password";
    // public static final String ENCODING = "encoding";
    // public static final String PANEL_CONNECTION_DELAY = "connectionDelay";

    private @Nullable String hostname;
    private int port = 1000;
    private int id = 1;
    private String password = "5678";
    private String encoding = "utf-8";
    private int connectionDelay = 0;

    public @Nullable String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public int getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public String getEncoding() {
        return encoding;
    }

    public int getConnectionDelay() {
        return connectionDelay;
    }
}
