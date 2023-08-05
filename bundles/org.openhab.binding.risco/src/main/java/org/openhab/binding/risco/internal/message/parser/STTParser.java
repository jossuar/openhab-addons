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
package org.openhab.binding.risco.internal.message.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.ThingProperty;

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
    public ThingProperty[] parse(String s) {
        List<ThingProperty> data = new ArrayList<ThingProperty>();

        /*
         * for (STTProperty prop : properties) {
         * String value;
         * if (s.contains(prop.flag)) {
         * value = "true";
         * } else {
         * value = "false";
         * }
         * data.add(new ThingProperty(prop.property, value));
         * }
         */
        return data.toArray(new ThingProperty[0]);
    }
}
