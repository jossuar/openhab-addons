package org.openhab.binding.risco.internal.message.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.KeyValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NonNullByDefault
public class CLOCKParser implements CommandParser {
    private final Logger logger = LoggerFactory.getLogger(CLOCKParser.class);
    private final static Pattern PATTERN = Pattern.compile("^(\\d\\d)/(\\d\\d)/(\\d\\d\\d\\d) (\\d\\d):(\\d\\d)$");

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
