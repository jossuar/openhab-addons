/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.caddx.internal.handler;

import org.openhab.binding.caddx.internal.CaddxMessage;

/**
 * Used to parse panel log event messages.
 *
 * @author Georgios Moutsos - Initial contribution
 */
public class LogEventMessage {

    public final String number;
    public final String size;
    public final String type;
    public final String zud;
    public final String partition;
    public final String month;
    public final String day;
    public final String hour;
    public final String minute;

    LogEventMessage(CaddxMessage message) {
        this.number = message.getPropertyById("panel_log_event_number");
        this.size = message.getPropertyById("panel_log_event_size");
        this.type = message.getPropertyById("panel_log_event_type");
        this.zud = message.getPropertyById("panel_log_event_zud");
        this.partition = message.getPropertyById("panel_log_event_partition");
        this.month = message.getPropertyById("panel_log_event_month");
        this.day = message.getPropertyById("panel_log_event_day");
        this.hour = message.getPropertyById("panel_log_event_hour");
        this.minute = message.getPropertyById("panel_log_event_minute");
    }

    private final String alarmList[] = { "Alarm Zone", "Alarm restore Zone", "Bypass Zone", "Bypass restore Zone",
            "Tamper Zone", "Tamper restore Zone", "Trouble Zone", "Trouble restore Zone", "TX low battery Zone",
            "TX low battery restore Zone", "Zone lost Zone", "Zone lost restore Zone", "Start of cross time Zone",
            "Not used None", "Not used None", "Not used None", "Not used None", "Special expansion event None",
            "Duress None", "Manual fire None", "Auxiliary 2 panic None", "Not used None", "Panic None",
            "Keypad tamper None", "Control box tamper Device", "Control box tamper restore Device", "AC fail Device",
            "AC fail restore Device", "Low battery Device", "Low battery restore Device", "Over-current Device",
            "Over-current restore Device", "Siren tamper Device", "Siren tamper restore Device", "Telephone fault None",
            "Telephone fault restore None", "Expander trouble Device", "Expander trouble restore Device",
            "Fail to communicate None", "Log full None", "Opening User", "Closing User", "Exit error User",
            "Recent closing User", "Auto-test None", "Start program None", "End program None", "Start download None",
            "End download None", "Cancel User", "Ground fault None", "Ground fault restore None", "Manual test None",
            "Closed with zones bypassed User", "Start of listen in None", "Technician on site None",
            "Technician left None", "Control power up None", "58-", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "Not used None",
            "First to open User", "Last to close User", "PIN entered with bit 7 set User", "Begin walk-test None",
            "End walk-test None", "Re-exit None", "Output trip User", "Data lost None" };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        // Date
        sb.append(String.format("%02d", Integer.parseInt(day))).append('-')
                .append(String.format("%02d", Integer.parseInt(month))).append(' ')
                .append(String.format("%02d", Integer.parseInt(hour))).append(':')
                .append(String.format("%02d", Integer.parseInt(minute))).append(' ');

        int eventType = Integer.parseInt(type);
        if (eventType >= 0 && eventType <= 12) { // Zone, Yes
            sb.append(alarmList[eventType]).append(" Partition ").append(Integer.parseInt(partition) + 1)
                    .append(" Zone ").append(Integer.parseInt(zud) + 1);
        } else if ((eventType >= 17 && eventType <= 17) || (eventType >= 34 && eventType <= 35)
                || (eventType >= 38 && eventType <= 39) || (eventType >= 44 && eventType <= 48)
                || (eventType >= 50 && eventType <= 52) || (eventType >= 54 && eventType <= 57)
                || (eventType >= 123 && eventType <= 124) || (eventType >= 127 && eventType <= 127)) { // None, No
            sb.append(alarmList[eventType]);
        } else if ((eventType >= 18 && eventType <= 20) || (eventType >= 22 && eventType <= 23)
                || (eventType >= 125 && eventType <= 125)) { // None, Yes
            sb.append(alarmList[eventType]).append(" Partition ").append(Integer.parseInt(partition) + 1);
        } else if ((eventType >= 24 && eventType <= 33) || (eventType >= 36 && eventType <= 37)) { // Device, No
            sb.append(alarmList[eventType]).append(" Device ").append(zud);
        } else if ((eventType >= 40 && eventType <= 43) || (eventType >= 49 && eventType <= 49)
                || (eventType >= 53 && eventType <= 53) || (eventType >= 120 && eventType <= 122)) { // User, Yes
            sb.append(alarmList[eventType]).append(" Partition ").append(Integer.parseInt(partition) + 1)
                    .append(" User ").append(Integer.parseInt(zud) + 1);
        } else if ((eventType >= 126 && eventType <= 126)) { // User, No
            sb.append(alarmList[eventType]).append(" User ").append(Integer.parseInt(zud) + 1);
        }

        sb.append("");

        return sb.toString();
    }

    public String getProperty() {
        return "panel_log_message_" + String.format("%02d", Integer.parseInt(number));
    }
}
