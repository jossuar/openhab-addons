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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.KeyValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class CLOCKParser implements CommandParser {
    private static final Pattern PATTERN = Pattern.compile("^(\\d\\d)/(\\d\\d)/(\\d\\d\\d\\d) (\\d\\d):(\\d\\d)$");
    private final Logger logger = LoggerFactory.getLogger(CLOCKParser.class);

    @Override
    public KeyValuePair[] parse(String s) {
        List<KeyValuePair> data = new ArrayList<KeyValuePair>();

        Matcher m = PATTERN.matcher(s);
        if (m.matches()) {
            data.add(new KeyValuePair("day", m.group(1)));
            data.add(new KeyValuePair("month", m.group(2)));
            data.add(new KeyValuePair("year", m.group(3)));
            data.add(new KeyValuePair("hour", m.group(4)));
            data.add(new KeyValuePair("minute", m.group(5)));
        } else {
            logger.debug("Not expected CLOCK Value [{}]", s);
        }

        return data.toArray(new KeyValuePair[0]);
    }
}
