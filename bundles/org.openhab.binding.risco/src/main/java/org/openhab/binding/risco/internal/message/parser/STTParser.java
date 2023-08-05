package org.openhab.binding.risco.internal.message.parser;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.ThingProperty;

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
