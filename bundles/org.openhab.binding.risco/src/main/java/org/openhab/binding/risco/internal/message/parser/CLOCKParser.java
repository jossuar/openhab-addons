package org.openhab.binding.risco.internal.message.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.ThingProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class CLOCKParser implements CommandParser {
    private final Logger logger = LoggerFactory.getLogger(CLOCKParser.class);
    private final static Pattern PATTERN = Pattern.compile("^(\\d\\d)/(\\d\\d)/(\\d\\d\\d\\d) (\\d\\d):(\\d\\d)$");

    @Override
    public ThingProperty[] parse(String s) {
        List<ThingProperty> data = new ArrayList<ThingProperty>();

        /*
         * Matcher m = PATTERN.matcher(s);
         * if (m.matches()) {
         * data.add(new ThingProperty("day", m.group(1)));
         * data.add(new ThingProperty("month", m.group(2)));
         * data.add(new ThingProperty("year", m.group(3)));
         * data.add(new ThingProperty("hour", m.group(4)));
         * data.add(new ThingProperty("minute", m.group(5)));
         * } else {
         * logger.debug("Not expected CLOCK Value [{}]", s);
         * }
         */
        return data.toArray(new ThingProperty[0]);
    }
}
