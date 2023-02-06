package org.openhab.binding.risco.internal.message;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoBindingConstants;

@NonNullByDefault
public enum RiscoMessageType {
    UNKNOWN("", "", false, "unknown", "Unknown"),

    STATUS_SYSTEM("SSTT", RiscoBindingConstants.SYSTEM, false, "system", "System",
            new RiscoMessageTypeProperty("low_battery_trouble", "B"), new RiscoMessageTypeProperty("ac_trouble", "A"),
            new RiscoMessageTypeProperty("phone_line_trouble", "P"), new RiscoMessageTypeProperty("clock_trouble", "C"),
            new RiscoMessageTypeProperty("default_switch", "D"),
            new RiscoMessageTypeProperty("ms1_report_trouble", "1"),
            new RiscoMessageTypeProperty("ms2_report_trouble", "2"),
            new RiscoMessageTypeProperty("ms3_report_trouble", "3"), new RiscoMessageTypeProperty("box_tamper", "X"),
            new RiscoMessageTypeProperty("jamming_trouble", "J"), new RiscoMessageTypeProperty("prog_mode", "I"),
            new RiscoMessageTypeProperty("learn_mode", "L"), new RiscoMessageTypeProperty("three_min_bypass", "M"),
            new RiscoMessageTypeProperty("walk_test", "W"), new RiscoMessageTypeProperty("aux_trouble", "U"),
            new RiscoMessageTypeProperty("rs485_bus_trouble", "R"), new RiscoMessageTypeProperty("ls_switch", "S"),
            new RiscoMessageTypeProperty("bell_switch", "F"), new RiscoMessageTypeProperty("bell_trouble", "E"),
            new RiscoMessageTypeProperty("bell_tamper", "Y"), new RiscoMessageTypeProperty("service_expired", "V"),
            new RiscoMessageTypeProperty("payment_expired", "T"), new RiscoMessageTypeProperty("service_mode", "Z"),
            new RiscoMessageTypeProperty("dual_path", "Q"), new RiscoMessageTypeProperty("bus_speed", "H")),

    STATUS_PARTITION("PSTT", RiscoBindingConstants.PARTITION, true, "partition{}", "Partition {}",
            new RiscoMessageTypeProperty("duress", "D"), new RiscoMessageTypeProperty("false_code", "C"),
            new RiscoMessageTypeProperty("fire", "F"), new RiscoMessageTypeProperty("panic", "P"),
            new RiscoMessageTypeProperty("medic", "M"), new RiscoMessageTypeProperty("arm", "A"),
            new RiscoMessageTypeProperty("home_stay", "H"), new RiscoMessageTypeProperty("ready_to_arm", "R"),
            new RiscoMessageTypeProperty("exists", "E"), new RiscoMessageTypeProperty("reset_required", "S"),
            new RiscoMessageTypeProperty("no_activity_alert", "N"), new RiscoMessageTypeProperty("group_a_arm", "1"),
            new RiscoMessageTypeProperty("group_b_arm", "2"), new RiscoMessageTypeProperty("group_c_arm", "3"),
            new RiscoMessageTypeProperty("group_d_arm", "4"), new RiscoMessageTypeProperty("trouble", "T")),

    STATUS_ZONE("ZSTT", RiscoBindingConstants.ZONE, true, "zone%d", "Zone %d",
            new RiscoMessageTypeProperty("open", "O"), new RiscoMessageTypeProperty("arm", "A"),
            new RiscoMessageTypeProperty("alarm", "a"), new RiscoMessageTypeProperty("tamper", "T"),
            new RiscoMessageTypeProperty("trouble", "R"), new RiscoMessageTypeProperty("lost", "L"),
            new RiscoMessageTypeProperty("low_battery", "B"), new RiscoMessageTypeProperty("bypass", "Y"),
            new RiscoMessageTypeProperty("communication_trouble", "C"), new RiscoMessageTypeProperty("soak_test", "S"),
            new RiscoMessageTypeProperty("hours24", "H"), new RiscoMessageTypeProperty("not_used", "N"),
            new RiscoMessageTypeProperty("exists", "E")),

    STATUS_OUTPUT("OSTT", RiscoBindingConstants.OUTPUT, true, "output{}", "Output {}",
            new RiscoMessageTypeProperty("output_is_active", "a"), new RiscoMessageTypeProperty("exists", "E")),

    STATUS_UTILITY_OUTPUT("UOSTT", RiscoBindingConstants.UTILITY_OUTPUT, true, "utility_output{}", "Utility Output {}",
            new RiscoMessageTypeProperty("output_is_active", "a")),

    STATUS_KEYPAD("KPSTT", RiscoBindingConstants.KEYPAD, true, "keypad{}", "Keypad {}",
            new RiscoMessageTypeProperty("low_battery_trouble", "B"), new RiscoMessageTypeProperty("box_tamper", "T"),
            new RiscoMessageTypeProperty("communication_trouble", "C"), new RiscoMessageTypeProperty("lost", "L"),
            new RiscoMessageTypeProperty("exists", "E")),

    STATUS_KEYFOB("KFSTT", RiscoBindingConstants.KEYFOB, true, "keyfob{}", "Keyfob {}",
            new RiscoMessageTypeProperty("low_battery_trouble", "B"), new RiscoMessageTypeProperty("exists", "E")),

    STATUS_BUS_EXPANDER("BESTT", RiscoBindingConstants.BUS_EXPANDER, true, "bus_expander{}", "Bus Expander {}",
            new RiscoMessageTypeProperty("tamper", "T"), new RiscoMessageTypeProperty("communication_trouble", "C"),
            new RiscoMessageTypeProperty("exists", "E")),

    STATUS_ZONE_EXPANDER("ZESTT", RiscoBindingConstants.ZONE_EXPANDER, true, "zone_expander{}", "Zone Expander {}",
            new RiscoMessageTypeProperty("tamper", "T"), new RiscoMessageTypeProperty("communication_trouble", "C"),
            new RiscoMessageTypeProperty("aux_trouble", "A"), new RiscoMessageTypeProperty("exists", "E")),

    STATUS_OUTPUT_EXPANDER("OESTT", RiscoBindingConstants.OUTPUT_EXPANDER, true, "output_expander{}",
            "Output Expander {}", new RiscoMessageTypeProperty("tamper", "T"),
            new RiscoMessageTypeProperty("communication_trouble", "C"),
            new RiscoMessageTypeProperty("phone_line_trouble", "P"),
            new RiscoMessageTypeProperty("dual_path_trouble", "D"), new RiscoMessageTypeProperty("exists", "E")),

    STATUS_WIRELESS_MODULE("WMSTT", RiscoBindingConstants.WIRELESS_MODULE, true, "wireless_module{}",
            "Wireless Module {}", new RiscoMessageTypeProperty("tamper", "T"),
            new RiscoMessageTypeProperty("communication_trouble", "C"), new RiscoMessageTypeProperty("jamming", "J"),
            new RiscoMessageTypeProperty("lost_signal", "L"), new RiscoMessageTypeProperty("exists", "E")),

    STATUS_VOICE_MODULE("VMSTT", RiscoBindingConstants.VOICE_MODULE, true, "voice_module{}", "Voice Module {}",
            new RiscoMessageTypeProperty("tamper", "T"), new RiscoMessageTypeProperty("communication_trouble", "C"),
            new RiscoMessageTypeProperty("exists", "E")),

    STATUS_CELLULAR_ON_BUS("COBSTT", RiscoBindingConstants.CELLULAR_ON_BUS, true, "cellular_on_bus{}",
            "Cellular On Bus {}", new RiscoMessageTypeProperty("tamper", "T"),
            new RiscoMessageTypeProperty("comm_trouble", "C"), new RiscoMessageTypeProperty("low_battery", "B"),
            new RiscoMessageTypeProperty("charge_trouble", "G"),
            new RiscoMessageTypeProperty("multiple_gsm_installed", "W"),
            new RiscoMessageTypeProperty("ac_trouble", "A"), new RiscoMessageTypeProperty("exists", "E")),

    STATUS_SIREN("SNSTT", RiscoBindingConstants.SIREN, true, "siren{}", "Siren {}",
            new RiscoMessageTypeProperty("radio_low_battery_trouble", "R"),
            new RiscoMessageTypeProperty("speaker_low_battery_trouble", "S"),
            new RiscoMessageTypeProperty("battery_load", "O"),
            new RiscoMessageTypeProperty("communication_trouble", "C"), new RiscoMessageTypeProperty("exists", "E"),
            new RiscoMessageTypeProperty("proximity_tamper", "P"), new RiscoMessageTypeProperty("aux_trouble", "U"),
            new RiscoMessageTypeProperty("speaker_flt", "N"), new RiscoMessageTypeProperty("charge_trouble", "G"),
            new RiscoMessageTypeProperty("invalid", "I"), new RiscoMessageTypeProperty("box_tamper", "T"),
            new RiscoMessageTypeProperty("lost", "L"), new RiscoMessageTypeProperty("low_battery", "W"))

    ;

    public final String commandName;
    public final String thingType;
    public final boolean hasIndex;
    public final String thingIdFormat;
    public final String thingLabelFormat;
    public final RiscoMessageTypeProperty[] properties;

    RiscoMessageType(String commandName, String thingType, boolean hasIndex, String thingIdFormat,
            String thingLabelFormat, RiscoMessageTypeProperty... properties) {
        this.commandName = commandName;
        this.thingType = thingType;
        this.hasIndex = hasIndex;
        this.thingIdFormat = thingIdFormat;
        this.thingLabelFormat = thingLabelFormat;
        this.properties = properties;
    }

    private static final Map<String, RiscoMessageType> BY_MESSAGE_TYPE = new HashMap<>();
    private static final List<String> BY_MESSAGE_LENGTH = Arrays.asList();

    static {
        for (RiscoMessageType mt : values()) {
            BY_MESSAGE_TYPE.put(mt.commandName, mt);
            BY_MESSAGE_LENGTH.add(mt.commandName);
        }

        Comparator<String> comparator = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                Integer L1 = o1.length();
                return L1.compareTo(o2.length()) * -1;
            }
        };

        Collections.sort(BY_MESSAGE_LENGTH, comparator);
    }

    public static @Nullable RiscoMessageType valueOfMessage(String commandName) {
        return BY_MESSAGE_TYPE.get(commandName);
    }

    public static @Nullable RiscoMessageType valueOfCommand(String commandName) {
        for (String name : BY_MESSAGE_LENGTH) {
            if (name.equals(commandName)) {
                return BY_MESSAGE_TYPE.get(commandName);
            }
        }

        return null;
    }
}
