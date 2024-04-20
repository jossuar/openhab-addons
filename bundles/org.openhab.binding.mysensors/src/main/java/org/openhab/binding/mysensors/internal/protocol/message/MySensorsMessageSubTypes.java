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
package org.openhab.binding.mysensors.internal.protocol.message;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * SubType categories:
 *
 * S == sensor
 * V == variable
 * I == internal
 *
 * @author Tim Oberföll - Initial contribution
 *
 */
@NonNullByDefault
public enum MySensorsMessageSubTypes {
    S,
    V,
    I;
}
