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
package org.openhab.binding.risco.internal.message.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.KeyValuePair;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class STTParser implements CommandParser {

    public final STTProperty[] properties;

    public STTParser(STTProperty... properties) {
        this.properties = properties;
    }

    @Override
    public KeyValuePair[] parse(String s) {
        List<KeyValuePair> data = new ArrayList<KeyValuePair>();

        for (STTProperty prop : properties) {
            String value;
            if (s.contains(prop.flag)) {
                value = "true";
            } else {
                value = "false";
            }
            data.add(new KeyValuePair(prop.property, value));
        }

        return data.toArray(new KeyValuePair[0]);
    }
}
