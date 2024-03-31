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
 * Configuration class for the Risco Partition Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoPartitionConfiguration {

    // Partition Thing constants
    public static final String PARTITION_NUMBER = "partitionNumber";

    // Partition Thing parameters
    private int partitionNumber;
    private int userIndex = 1;

    public int getPartitionNumber() {
        return partitionNumber;
    }

    public int getUserIndex() {
        return userIndex;
    }
}
