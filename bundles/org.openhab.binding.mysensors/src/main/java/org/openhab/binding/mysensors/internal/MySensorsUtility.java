/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.mysensors.internal;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Class gives some utility methods that not belong to a specific class
 *
 * @author Andrea Cioni - Initial contribution
 *
 */
@NonNullByDefault
public class MySensorsUtility {
    /**
     * Invert a generics map swapping key with value
     *
     * @param map the map to be inverted
     * @param hasDuplicate if true only one value (randomly) will be used as key in map that contains same value for
     *            different keys.
     * @return the new inverted map
     */
    public static <V, K> Map<V, K> invertMap(Map<K, V> map, boolean hasDuplicate) {
        if (!hasDuplicate) {
            return map.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey));
        } else {
            return map.entrySet().stream().collect(Collectors.toMap(Entry::getValue, Entry::getKey, (a, b) -> a));
        }
    }
}
