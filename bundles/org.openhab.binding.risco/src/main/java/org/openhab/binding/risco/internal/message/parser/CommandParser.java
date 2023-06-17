package org.openhab.binding.risco.internal.message.parser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.KeyValuePair;

@NonNullByDefault
public interface CommandParser {
    KeyValuePair[] parse(String s);
}
