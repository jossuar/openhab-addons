package org.openhab.binding.risco.internal.message.parser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.ThingProperty;

@NonNullByDefault
public class NULLParser implements CommandParser {

    @Override
    public ThingProperty[] parse(String s) {
        return new ThingProperty[0];
    }
}
