package org.openhab.binding.risco.internal.message;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.message.parser.CLOCKParser;
import org.openhab.binding.risco.internal.message.parser.CommandParser;
import org.openhab.binding.risco.internal.message.parser.DTYPZParser;
import org.openhab.binding.risco.internal.message.parser.STTParser;
import org.openhab.binding.risco.internal.message.parser.STTProperty;

@NonNullByDefault
public enum RiscoMessageType {

    // @formatter:off
    UNKNOWN("", "", false, "unknown", "Unknown", new STTParser()),

    CLOCK("CLOCK", "", false, "","", new CLOCKParser()),

    STATUS_SYSTEM("SSTT", RiscoBindingConstants.SYSTEM, false, RiscoBindingConstants.SYSTEM, "System",
            new STTParser(
                    new STTProperty("low_battery_trouble", "B"),
                    new STTProperty("ac_trouble", "A"),
                    new STTProperty("phone_line_trouble", "P"),
                    new STTProperty("clock_trouble", "C"),
                    new STTProperty("default_switch", "D"),
                    new STTProperty("ms1_report_trouble", "1"),
                    new STTProperty("ms2_report_trouble", "2"),
                    new STTProperty("ms3_report_trouble", "3"),
                    new STTProperty("box_tamper", "X"),
                    new STTProperty("jamming_trouble", "J"),
                    new STTProperty("prog_mode", "I"),
                    new STTProperty("learn_mode", "L"),
                    new STTProperty("three_min_bypass", "M"),
                    new STTProperty("walk_test", "W"),
                    new STTProperty("aux_trouble", "U"),
                    new STTProperty("rs485_bus_trouble", "R"),
                    new STTProperty("ls_switch", "S"),
                    new STTProperty("bell_switch", "F"),
                    new STTProperty("bell_trouble", "E"),
                    new STTProperty("bell_tamper", "Y"),
                    new STTProperty("service_expired", "V"),
                    new STTProperty("payment_expired", "T"),
                    new STTProperty("service_mode", "Z"),
                    new STTProperty("dual_path", "Q"),
                    new STTProperty("bus_speed", "H"))),

    STATUS_PARTITION("PSTT", RiscoBindingConstants.PARTITION, true, RiscoBindingConstants.PARTITION + "%d", "Partition %d",
            new STTParser(
                    new STTProperty("duress", "D"),
                    new STTProperty("false_code", "C"),
                    new STTProperty("fire", "F"),
                    new STTProperty("panic", "P"),
                    new STTProperty("medic", "M"),
                    new STTProperty("arm", "A"),
                    new STTProperty("home_stay", "H"),
                    new STTProperty("ready_to_arm", "R"),
                    new STTProperty("exists", "E"),
                    new STTProperty("reset_required", "S"),
                    new STTProperty("no_activity_alert", "N"),
                    new STTProperty("group_a_arm", "1"),
                    new STTProperty("group_b_arm", "2"),
                    new STTProperty("group_c_arm", "3"),
                    new STTProperty("group_d_arm", "4"),
                    new STTProperty("trouble", "T"))),

    STATUS_ZONE("ZSTT", RiscoBindingConstants.ZONE, true, RiscoBindingConstants.ZONE + "%d", "Zone %d",
            new STTParser(
                    new STTProperty("open", "O"),
                    new STTProperty("arm", "A"),
                    new STTProperty("alarm", "a"),
                    new STTProperty("tamper", "T"),
                    new STTProperty("trouble", "R"),
                    new STTProperty("lost", "L"),
                    new STTProperty("low_battery", "B"),
                    new STTProperty("bypass", "Y"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("soak_test", "S"),
                    new STTProperty("hours24", "H"),
                    new STTProperty("not_used", "N"),
                    new STTProperty("exists", "E"))),

    STATUS_OUTPUT("OSTT", RiscoBindingConstants.OUTPUT, true, RiscoBindingConstants.OUTPUT + "%d", "Output %d",
            new STTParser(
                    new STTProperty("output_is_active", "a"),
                    new STTProperty("exists", "E"))),

    STATUS_UTILITY_OUTPUT("UOSTT", RiscoBindingConstants.UTILITY_OUTPUT, true, RiscoBindingConstants.UTILITY_OUTPUT + "%d", "Utility Output %d",
            new STTParser(
                    new STTProperty("output_is_active", "a"))),

    STATUS_KEYPAD("KPSTT", RiscoBindingConstants.KEYPAD, true, RiscoBindingConstants.KEYPAD + "%d", "Keypad %d",
            new STTParser(
                    new STTProperty("low_battery_trouble", "B"),
                    new STTProperty("box_tamper", "T"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("lost", "L"),
                    new STTProperty("exists", "E"))),

    STATUS_KEYFOB("KFSTT", RiscoBindingConstants.KEYFOB, true, RiscoBindingConstants.KEYFOB + "%d", "Keyfob %d",
            new STTParser(
                    new STTProperty("low_battery_trouble", "B"),
                    new STTProperty("exists", "E"))),

    STATUS_BUS_EXPANDER("BESTT", RiscoBindingConstants.BUS_EXPANDER, true, RiscoBindingConstants.BUS_EXPANDER + "%d", "Bus Expander %d",
            new STTParser(
                    new STTProperty("tamper", "T"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("exists", "E"))),

    STATUS_ZONE_EXPANDER("ZESTT", RiscoBindingConstants.ZONE_EXPANDER, true, RiscoBindingConstants.ZONE_EXPANDER + "%d", "Zone Expander %d",
            new STTParser(
                    new STTProperty("tamper", "T"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("aux_trouble", "A"),
                    new STTProperty("exists", "E"))),

    STATUS_OUTPUT_EXPANDER("OESTT", RiscoBindingConstants.OUTPUT_EXPANDER, true, RiscoBindingConstants.OUTPUT_EXPANDER + "%d", "Output Expander %d",
            new STTParser(
                    new STTProperty("tamper", "T"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("phone_line_trouble", "P"),
                    new STTProperty("dual_path_trouble", "D"),
                    new STTProperty("exists", "E"))),

    STATUS_WIRELESS_MODULE("WMSTT", RiscoBindingConstants.WIRELESS_MODULE, true, RiscoBindingConstants.WIRELESS_MODULE + "%d", "Wireless Module %d",
            new STTParser(
                    new STTProperty("tamper", "T"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("jamming", "J"),
                    new STTProperty("lost_signal", "L"),
                    new STTProperty("exists", "E"))),

    STATUS_VOICE_MODULE("VMSTT", RiscoBindingConstants.VOICE_MODULE, true, RiscoBindingConstants.VOICE_MODULE + "%d", "Voice Module %d",
            new STTParser(
                    new STTProperty("tamper", "T"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("exists", "E"))),

    STATUS_CELLULAR_ON_BUS("COBSTT", RiscoBindingConstants.CELLULAR_ON_BUS, true, RiscoBindingConstants.CELLULAR_ON_BUS + "%d", "Cellular On Bus %d",
            new STTParser(
                    new STTProperty("tamper", "T"),
                    new STTProperty("comm_trouble", "C"),
                    new STTProperty("low_battery", "B"),
                    new STTProperty("charge_trouble", "G"),
                    new STTProperty("multiple_gsm_installed", "W"),
                    new STTProperty("ac_trouble", "A"),
                    new STTProperty("exists", "E"))),

    STATUS_SIREN("SNSTT", RiscoBindingConstants.SIREN, true, RiscoBindingConstants.SIREN + "%d", "Siren %d",
            new STTParser(
                    new STTProperty("radio_low_battery_trouble", "R"),
                    new STTProperty("speaker_low_battery_trouble", "S"),
                    new STTProperty("battery_load", "O"),
                    new STTProperty("communication_trouble", "C"),
                    new STTProperty("exists", "E"),
                    new STTProperty("proximity_tamper", "P"),
                    new STTProperty("aux_trouble", "U"),
                    new STTProperty("speaker_flt", "N"),
                    new STTProperty("charge_trouble", "G"),
                    new STTProperty("invalid", "I"),
                    new STTProperty("box_tamper", "T"),
                    new STTProperty("lost", "L"),
                    new STTProperty("low_battery", "W"))),

    COMMAND_DTYPZ("DTYPZ", RiscoBindingConstants.ZONE, true, RiscoBindingConstants.ZONE + "%d", "Zone %d",
            new DTYPZParser())

    ;
    // @formatter:on

    public final String commandName;
    public final String thingType;
    public final boolean hasIndex;
    public final String thingIdFormat;
    public final String thingLabelFormat;
    public final CommandParser parser;

    RiscoMessageType(String commandName, String thingType, boolean hasIndex, String thingIdFormat,
            String thingLabelFormat, CommandParser parser) {
        this.commandName = commandName;
        this.thingType = thingType;
        this.hasIndex = hasIndex;
        this.thingIdFormat = thingIdFormat;
        this.thingLabelFormat = thingLabelFormat;
        this.parser = parser;
    }

    private static final Map<String, RiscoMessageType> BY_MESSAGE_TYPE = new HashMap<>();

    static {
        for (RiscoMessageType mt : values()) {
            BY_MESSAGE_TYPE.put(mt.commandName, mt);
        }

        Comparator<String> comparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Integer L1 = o1.length();
                return L1.compareTo(o2.length()) * -1;
            }
        };
    }

    public @Nullable static RiscoMessageType valueOfMessage(String commandName) {
        return BY_MESSAGE_TYPE.get(commandName);
    }
}
