package org.openhab.binding.risco.internal.message.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.KeyValuePair;

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
