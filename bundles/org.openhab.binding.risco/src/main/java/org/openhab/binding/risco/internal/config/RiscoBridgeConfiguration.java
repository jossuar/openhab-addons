/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
    public static final String LISTENING_PORT = "listeningPort";
    public static final String PANEL_IP = "panelIp";
    public static final String PANEL_PORT = "panelPort";
    public static final String PANEL_ID = "panelId";
    public static final String PANEL_PASSWORD = "panelPassword";
    public static final String ENCODING = "encoding";
    public static final String CLOUD_URL = "cloudUrl";
    public static final String CLOUD_PORT = "cloudPort";
    public static final String PANEL_CONNECTION_DELAY = "panelConnectionDelay";
    public static final String SOCKET_MODE = "socketMode";

    private int listeningPort = 33000;
    private @Nullable String panelIp;
    private int panelPort = 1000;
    private int panelId = 1;
    private boolean guessPasswordAndPanelId = false;
    private @Nullable String panelPassword;
    private String encoding = "utf-8";
    private String cloudUrl = "www.riscocloud.com";
    private int cloudPort = 33000;
    private int panelConnectionDelay = 30000;

    public int getListeningPort() {
        return listeningPort;
    }

    public @Nullable String getPanelIp() {
        return panelIp;
    }

    public int getPanelPort() {
        return panelPort;
    }

    public int getPanelId() {
        return panelId;
    }

    public boolean isGuessPasswordAndPanelId() {
        return guessPasswordAndPanelId;
    }

    public @Nullable String getPanelPassword() {
        return panelPassword;
    }

    public String getEncoding() {
        return encoding;
    }

    public @Nullable String getCloudUrl() {
        return cloudUrl;
    }

    public int getCloudPort() {
        return cloudPort;
    }

    public int getPanelConnectionDelay() {
        return panelConnectionDelay;
    }
}
