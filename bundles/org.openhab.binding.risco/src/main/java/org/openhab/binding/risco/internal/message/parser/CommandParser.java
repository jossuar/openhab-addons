package org.openhab.binding.risco.internal.message.parser;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.message.ThingProperty;

@NonNullByDefault
public interface CommandParser {
    ThingProperty[] parse(String s);
}
