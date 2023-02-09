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
public class DTYPZValueParser implements ValueParser {
    private final Logger logger = LoggerFactory.getLogger(DTYPZValueParser.class);
    private final static Pattern PATTERN = Pattern.compile("^(\\d+),(\\d+),(\\d+),(\\d+),([A-Z]+)$");

    @Override
    public KeyValuePair[] parse(String s) {
        List<KeyValuePair> data = new ArrayList<KeyValuePair>();

        Matcher m = PATTERN.matcher(s);
        if (m.matches()) {
            if ("255".equals(m.group(1))) {
                data.add(new KeyValuePair("channel",
                        m.group(5) + " " + "0" + ":" + String.format("%02d", Integer.valueOf(m.group(3))) + ":"
                                + String.format("%02d", Integer.valueOf(m.group(2)))));
            } else {
                data.add(new KeyValuePair("channel",
                        m.group(5) + " " + (Integer.valueOf(m.group(1)) + 1) + ":"
                                + String.format("%02d", Integer.valueOf(m.group(3))) + ":"
                                + String.format("%02d", Integer.valueOf(m.group(2)))));
            }
        } else {
            data.add(new KeyValuePair("channel", "- 0:00:00"));
            logger.debug("Not expected DTYPZ Value [{}]", s);
        }

        return (KeyValuePair[]) data.toArray();
    }
}
