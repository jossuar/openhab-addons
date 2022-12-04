package org.openhab.binding.risco.internal.message;

public class RiscoMessageTypeProperty {
    public String property;
    public String flag;

    public RiscoMessageTypeProperty(String property, String flag) {
        super();

        this.property = property;
        this.flag = flag;
    }
}
