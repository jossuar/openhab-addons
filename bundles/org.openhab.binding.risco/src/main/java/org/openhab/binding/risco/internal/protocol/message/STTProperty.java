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
package org.openhab.binding.risco.internal.protocol.message;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class STTProperty {
    public String property;
    public String flag;

    public STTProperty(String property, String flag) {
        super();

        this.property = property;
        this.flag = flag;
    }
}
