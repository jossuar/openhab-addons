package org.openhab.binding.risco.internal.message.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.ThingProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class DTYPZParser implements CommandParser {
    private final Logger logger = LoggerFactory.getLogger(DTYPZParser.class);
    private final static Pattern PATTERN = Pattern.compile("^(\\d+),(\\d+),(\\d+),(\\d+),([A-Z]+)$");

    @Override
    public ThingProperty[] parse(String s) {
        List<ThingProperty> data = new ArrayList<ThingProperty>();

        /*
         * Matcher m = PATTERN.matcher(s);
         * if (m.matches()) {
         * if ("255".equals(m.group(1))) {
         * data.add(new ThingProperty("channel",
         * m.group(5) + " " + "0" + ":" + String.format("%02d", Integer.valueOf(m.group(3))) + ":"
         * + String.format("%02d", Integer.valueOf(m.group(2)))));
         * } else {
         * data.add(new ThingProperty("channel",
         * m.group(5) + " " + (Integer.valueOf(m.group(1)) + 1) + ":"
         * + String.format("%02d", Integer.valueOf(m.group(3))) + ":"
         * + String.format("%02d", Integer.valueOf(m.group(2)))));
         * }
         * } else {
         * data.add(new ThingProperty("channel", "- 0:00:00"));
         * logger.debug("Not expected DTYPZ Value [{}]", s);
         * }
         */
        return data.toArray(new ThingProperty[0]);
    }
}
