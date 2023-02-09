package org.openhab.binding.risco.internal.message.parser;

public class STTProperty {
    public String property;
    public String flag;

    public STTProperty(String property, String flag) {
        super();

        this.property = property;
        this.flag = flag;
    }
}
