package org.openhab.binding.risco.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingUID;

@NonNullByDefault
public class ThingProperty {
    private final ThingUID thingUID;
    private final String name;
    private final String value;

    public ThingProperty(ThingUID thingUID, String name, String value) {
        this.thingUID = thingUID;
        this.name = name;
        this.value = value;
    }

    public ThingUID getThingUID() {
        return thingUID;
    }

    public String getKey() {
        return name;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("{").append(thingUID.toString()).append(" - ").append(name).append(": ").append(value);
        return sb.toString();
    }
}
