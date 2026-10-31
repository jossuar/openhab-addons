/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.binding.risco.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The settings needed to connect to a panel.
 *
 * @param name used to name the threads of the connection, e.g. the bridge UID
 * @param connectionDelayMs pause between opening the connection and logging in, needed by older panels
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public record RiscoConnectionSettings(String name, String hostname, int port, int panelId, String encoding,
        String password, int connectionDelayMs) {

    @Override
    public String toString() {
        // The password must not end up in the logs
        return "RiscoConnectionSettings[" + hostname + ":" + port + ", panelId=" + panelId + "]";
    }
}
