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
package org.openhab.binding.risco.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * A class that represents the Risco Alarm Request-Response Message pair.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoMessagePair {
    private final RiscoMessage request;
    private @Nullable RiscoMessage response = null;

    public RiscoMessagePair(RiscoMessage request) {
        this.request = request;
    }

    public MessageOrigin getMessageOrigin() {
        return request.getMessageOrigin();
    }

    public RiscoMessage getRequest() {
        return request;
    }

    public boolean hasResponse() {
        return response != null;
    }

    public @Nullable RiscoMessage getResponse() {
        return response;
    }

    public void setResponse(RiscoMessage response) {
        this.response = response;
    }
}
