package org.openhab.binding.risco.internal.message.parser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.KeyValuePair;

@NonNullByDefault
public class NULLParser implements CommandParser {

    @Override
    public KeyValuePair[] parse(String s) {
        return new KeyValuePair[0];
    }
}
