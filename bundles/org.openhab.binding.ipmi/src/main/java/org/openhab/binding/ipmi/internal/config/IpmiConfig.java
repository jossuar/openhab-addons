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
package org.openhab.binding.ipmi.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link IpmiConfig} class contains fields mapping thing configuration parameters.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class IpmiConfig {
    public @NonNullByDefault({}) String gatewayAddress;
    public @NonNullByDefault({}) String user;
    public @NonNullByDefault({}) String password;
    public int timeout;
    public int port;

    public String getGatewayAddress() {
        return gatewayAddress;
    }

    public void setGatewayAddress(String gatewayAddress) {
        this.gatewayAddress = gatewayAddress;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
