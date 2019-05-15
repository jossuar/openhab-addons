# Caddx Binding

The Caddx binding is used for communicating with the Caddx alarm panels

It provides connectivity to the alarm panel via a RS-232 serial connection to the Caddx interface or directly to the NX8E.


## Supported Things

This binding supports the following Thing types

| Thing      | Thing Type | Description                                                            |
|------------|------------|------------------------------------------------------------------------|
| bridge     | Bridge     | The  RS-232 interface.                                                 |
| panel      | Thing      | The basic representation of the alarm System.                          |
| partition  | Thing      | Represents a controllable area within the alarm system.                |
| zone       | Thing      | Represents a physical device such as a door, window, or motion sensor. |
| keypad     | Thing      | Represents a keypad.                                                   |

## Discovery

The Caddx binding first searches for the serial ports which can communicate via the binary protocol and adds them to the discovery inbox.
The bridge discovery is started manually through PaperUI.  
After a bridge is discovered and available to openHAB, the binding will attempt to discover things and add them to the discovery inbox.

Note:
There is currently no support to discover the available keypads.

## Binding Configuration

No binding configuration required.

## Thing Configuration

The things can be configured either through the online configuration utility via discovery, or manually through the configuration file.
The following table shows the available configuration parameters for each thing.

<table>
	<tr><td><b>Thing</b></td><td><b>Configuration Parameters</b></td></tr>
	<tr><td>bridge</td><td><table><tr><td><b>serialPort</b> - Serial port for the bridge - Required.</td></tr><tr><td><b>baud</b> - Baud rate of the bridge - Not Required - default = 9600.</td></tr></table></td></tr>
    <tr><td>panel</td><td><table><tr><td><b>userCode</b> - User code for the alarm panel - Not Required.</td></tr></table></td></tr>
	<tr><td>partition</td><td><b>partitionNumber</b> - Partition number (1-8) - Required.</td></tr>
	<tr><td>zone</td><td><table><tr><td><b>zoneNumber</b> - Zone number (1-256) - Required.</td></tr></table></td></tr>
	<tr><td>keypad</td><td><b>keypadAddress</b> - Keypad address (1-8) - Required.</td></tr>
</table>

The binding can be configured manually if discovery is not used.  
A thing configuration file in the format 'bindingName.things' would need to be created, and placed in the 'conf/things' folder.  
Here is an example of a thing configuration file called 'caddx.things':

```perl
Bridge caddx:bridge:MyBridgeName [ serialPort="/dev/ttyUSB0", baud=38400 ] {
    Thing panel panel
    Thing partition partition1 [ partitionNumber=1 ]
    Thing zone zone1 [ zoneNumber=1 ]
    Thing zone zone9 [ zoneNumber=9 ]
    Thing zone zone10 [ zoneNumber=10 ]
    Thing zone zone11 [ zoneNumber=11 ]
    Thing zone zone12 [ zoneNumber=12 ]
    Thing zone zone13 [ zoneNumber=13 ]
    Thing zone zone14 [ zoneNumber=14 ]
    Thing zone zone15 [ zoneNumber=15 ]
    Thing zone zone21 [ zoneNumber=21 ]
    Thing zone zone22 [ zoneNumber=22 ]
    Thing zone zone23 [ zoneNumber=23 ]
    Thing zone zone24 [ zoneNumber=24 ]
    Thing zone zone25 [ zoneNumber=25 ]
    Thing keypad keypad [ keypadAddress=8 ]
}
```


## Channels

Caddx Alarm things support a variety of channels as seen below in the following table:

<table>
	<tr><td><b>Channel</b></td><td><b>Item Type</b></td><td><b>Description</b></td></tr>
	<tr><td>bridge_reset</td><td>Switch</td><td>Reset the bridge connection.</td></tr>
	<tr><td>send_command</td><td>Switch</td><td>Send a command.</td></tr>
	<tr><td>panel_firmware_version</td><td>String</td><td>Firmware version</td></tr>
	<tr><td>panel_interface_configuration_message</td><td>Switch</td><td>Interface Configuration Message</td></tr>
	<tr><td>panel_zone_status_message</td><td>Switch</td><td>Zone Status Message</td></tr>
	<tr><td>panel_zones_snapshot_message</td><td>Switch</td><td>Zones Snapshot Message</td></tr>
	<tr><td>panel_partition_status_message</td><td>Switch</td><td>Partition Status Message</td></tr>
	<tr><td>panel_partitions_snapshot_message</td><td>Switch</td><td>Partitions Snapshot Message</td></tr>
	<tr><td>panel_system_status_message</td><td>Switch</td><td>System Status Message</td></tr>
	<tr><td>panel_x10_message_received</td><td>Switch</td><td>X-10 Message Received</td></tr>
	<tr><td>panel_log_event_message</td><td>Switch</td><td>Log Event Message</td></tr>
	<tr><td>panel_keypad_message_received</td><td>Switch</td><td>Keypad Message Received</td></tr>
	<tr><td>panel_interface_configuration_request</td><td>Switch</td><td>Interface Configuration Request</td></tr>
	<tr><td>panel_zone_name_request</td><td>Switch</td><td>Zone Name Request</td></tr>
	<tr><td>panel_zone_status_request</td><td>Switch</td><td>Zone Status Request</td></tr>
	<tr><td>panel_zones_snapshot_request</td><td>Switch</td><td>Zones Snapshot Request</td></tr>
	<tr><td>panel_partition_status_request</td><td>Switch</td><td>Partition Status Request</td></tr>
	<tr><td>panel_partitions_snapshot_request</td><td>Switch</td><td>Partitions Snapshot Request</td></tr>
	<tr><td>panel_system_status_request</td><td>Switch</td><td>System Status Request</td></tr>
	<tr><td>panel_send_x10_message</td><td>Switch</td><td>Send X-10 Message</td></tr>
	<tr><td>panel_log_event_request</td><td>Switch</td><td>Log Event Request</td></tr>
	<tr><td>panel_send_keypad_text_message</td><td>Switch</td><td>Send Keypad Text Message</td></tr>
	<tr><td>panel_keypad_terminal_mode_request</td><td>Switch</td><td>Keypad Terminal Mode Request</td></tr>
	<tr><td>panel_program_data_request</td><td>Switch</td><td>Program Data Request</td></tr>
	<tr><td>panel_program_data_command</td><td>Switch</td><td>Program Data Command</td></tr>
	<tr><td>panel_user_information_request_with_pin</td><td>Switch</td><td>User Information Request with PIN</td></tr>
	<tr><td>panel_user_information_request_without_pin</td><td>Switch</td><td>User Information Request without PIN</td></tr>
	<tr><td>panel_set_user_code_command_with_pin</td><td>Switch</td><td>Set User Code Command with PIN</td></tr>
	<tr><td>panel_set_user_code_command_without_pin</td><td>Switch</td><td>Set User Code Command without PIN</td></tr>
	<tr><td>panel_set_user_authorization_command_with_pin</td><td>Switch</td><td>Set User Authorization Command with PIN</td></tr>
	<tr><td>panel_set_user_authorization_command_without_pin</td><td>Switch</td><td>Set User Authorization Command without PIN</td></tr>
	<tr><td>panel_store_communication_event_command</td><td>Switch</td><td>Store Communication Event Command</td></tr>
	<tr><td>panel_set_clock_calendar_command</td><td>Switch</td><td>Set Clock / Calendar Command</td></tr>
	<tr><td>panel_primary_keypad_function_with_pin</td><td>Switch</td><td>Primary Keypad Function with PIN</td></tr>
	<tr><td>panel_primary_keypad_function_without_pin</td><td>Switch</td><td>Primary Keypad Function without PIN</td></tr>
	<tr><td>panel_secondary_keypad_function</td><td>Switch</td><td>Secondary Keypad Function</td></tr>
	<tr><td>panel_zone_bypass_toggle</td><td>Switch</td><td>Zone Bypass Toggle</td></tr>
	<tr><td>partition_bypass_code_required</td><td>Switch</td><td>Bypass code required</td></tr>
	<tr><td>partition_fire_trouble</td><td>Switch</td><td>Fire trouble</td></tr>
	<tr><td>partition_fire</td><td>Switch</td><td>Fire</td></tr>
	<tr><td>partition_pulsing_buzzer</td><td>Switch</td><td>Pulsing Buzzer</td></tr>
	<tr><td>partition_tlm_fault_memory</td><td>Switch</td><td>TLM fault memory</td></tr>
	<tr><td>partition_armed</td><td>Switch</td><td>Armed</td></tr>
	<tr><td>partition_instant</td><td>Switch</td><td>Instant</td></tr>
	<tr><td>partition_previous_alarm</td><td>Switch</td><td>Previous Alarm</td></tr>
	<tr><td>partition_siren_on</td><td>Switch</td><td>Siren on</td></tr>
	<tr><td>partition_steady_siren_on</td><td>Switch</td><td>Steady siren on</td></tr>
	<tr><td>partition_alarm_memory</td><td>Switch</td><td>Alarm memory</td></tr>
	<tr><td>partition_tamper</td><td>Switch</td><td>Tamper</td></tr>
	<tr><td>partition_cancel_command_entered</td><td>Switch</td><td>Cancel command entered</td></tr>
	<tr><td>partition_code_entered</td><td>Switch</td><td>Code entered</td></tr>
	<tr><td>partition_cancel_pending</td><td>Switch</td><td>Cancel pending</td></tr>
	<tr><td>partition_silent_exit_enabled</td><td>Switch</td><td>Silent exit enabled</td></tr>
	<tr><td>partition_entryguard</td><td>Switch</td><td>Entryguard (stay mode)</td></tr>
	<tr><td>partition_chime_mode_on</td><td>Switch</td><td>Chime mode on</td></tr>
	<tr><td>partition_entry</td><td>Switch</td><td>Entry</td></tr>
	<tr><td>partition_delay_expiration_warning</td><td>Switch</td><td>Delay expiration warning</td></tr>
	<tr><td>partition_exit1</td><td>Switch</td><td>Exit1</td></tr>
	<tr><td>partition_exit2</td><td>Switch</td><td>Exit2</td></tr>
	<tr><td>partition_led_extinguish</td><td>Switch</td><td>LED extinguish</td></tr>
	<tr><td>partition_cross_timing</td><td>Switch</td><td>Cross timing</td></tr>
	<tr><td>partition_recent_closing_being_timed</td><td>Switch</td><td>Recent closing being timed</td></tr>
	<tr><td>partition_exit_error_triggered</td><td>Switch</td><td>Exit error triggered</td></tr>
	<tr><td>partition_auto_home_inhibited</td><td>Switch</td><td>Auto home inhibited</td></tr>
	<tr><td>partition_sensor_low_battery</td><td>Switch</td><td>Sensor low battery</td></tr>
	<tr><td>partition_sensor_lost_supervision</td><td>Switch</td><td>Sensor lost supervision</td></tr>
	<tr><td>partition_zone_bypassed</td><td>Switch</td><td>Zone bypassed</td></tr>
	<tr><td>partition_force_arm_triggered_by_auto_arm</td><td>Switch</td><td>Force arm triggered by auto arm</td></tr>
	<tr><td>partition_ready_to_arm</td><td>Switch</td><td>Ready to arm</td></tr>
	<tr><td>partition_ready_to_force_arm</td><td>Switch</td><td>Ready to force arm</td></tr>
	<tr><td>partition_valid_pin_accepted</td><td>Switch</td><td>Valid PIN accepted</td></tr>
	<tr><td>partition_chime_on</td><td>Switch</td><td>Chime on (sounding)</td></tr>
	<tr><td>partition_error_beep</td><td>Switch</td><td>Error beep (triple beep)</td></tr>
	<tr><td>partition_tone_on</td><td>Switch</td><td>Tone on (activation tone)</td></tr>
	<tr><td>partition_entry1</td><td>Switch</td><td>Entry 1</td></tr>
	<tr><td>partition_open_period</td><td>Switch</td><td>Open period</td></tr>
	<tr><td>partition_alarm_sent_using_phone_number_1</td><td>Switch</td><td>Alarm sent using phone number 1</td></tr>
	<tr><td>partition_alarm_sent_using_phone_number_2</td><td>Switch</td><td>Alarm sent using phone number 2</td></tr>
	<tr><td>partition_alarm_sent_using_phone_number_3</td><td>Switch</td><td>Alarm sent using phone number 3</td></tr>
	<tr><td>partition_cancel_report_is_in_the_stack</td><td>Switch</td><td>Cancel report is in the stack</td></tr>
	<tr><td>partition_keyswitch_armed</td><td>Switch</td><td>Keyswitch armed</td></tr>
	<tr><td>partition_delay_trip_in_progress</td><td>Switch</td><td>Delay Trip in progress (common zone)</td></tr>
	<tr><td>partition_primary_command</td><td>Number</td><td>Partition Primary Command</td></tr>
	<tr><td>partition_secondary_command</td><td>Number</td><td>Partition Secondary Command</td></tr>
	<tr><td>keypad_ready_led</td><td>Number</td><td>Keypad Ready LED (0=Off, 1=On, 2=Flashing)</td></tr>
	<tr><td>zone_partition1</td><td>Switch</td><td>Partition 1</td></tr>
	<tr><td>zone_partition2</td><td>Switch</td><td>Partition 2</td></tr>
	<tr><td>zone_partition3</td><td>Switch</td><td>Partition 3</td></tr>
	<tr><td>zone_partition4</td><td>Switch</td><td>Partition 4</td></tr>
	<tr><td>zone_partition5</td><td>Switch</td><td>Partition 5</td></tr>
	<tr><td>zone_partition6</td><td>Switch</td><td>Partition 6</td></tr>
	<tr><td>zone_partition7</td><td>Switch</td><td>Partition 7</td></tr>
	<tr><td>zone_partition8</td><td>Switch</td><td>Partition 8</td></tr>
	<tr><td>zone_fire</td><td>Switch</td><td>Fire</td></tr>
	<tr><td>zone_24hour</td><td>Switch</td><td>24 Hour</td></tr>
	<tr><td>zone_key_switch</td><td>Switch</td><td>Key-switch</td></tr>
	<tr><td>zone_follower</td><td>Switch</td><td>Follower</td></tr>
	<tr><td>zone_entry_exit_delay_1</td><td>Switch</td><td>Entry / exit delay 1</td></tr>
	<tr><td>zone_entry_exit_delay_2</td><td>Switch</td><td>Entry / exit delay 2</td></tr>
	<tr><td>zone_interior</td><td>Switch</td><td>Interior</td></tr>
	<tr><td>zone_local_only</td><td>Switch</td><td>Local only</td></tr>
	<tr><td>zone_keypad_sounder</td><td>Switch</td><td>Keypad Sounder</td></tr>
	<tr><td>zone_yelping_siren</td><td>Switch</td><td>Yelping siren</td></tr>
	<tr><td>zone_steady_siren</td><td>Switch</td><td>Steady siren</td></tr>
	<tr><td>zone_chime</td><td>Switch</td><td>Chime</td></tr>
	<tr><td>zone_bypassable</td><td>Switch</td><td>Bypassable</td></tr>
	<tr><td>zone_group_bypassable</td><td>Switch</td><td>Group bypassable</td></tr>
	<tr><td>zone_force_armable</td><td>Switch</td><td>Force armable</td></tr>
	<tr><td>zone_entry_guard</td><td>Switch</td><td>Entry guard</td></tr>
	<tr><td>zone_fast_loop_response</td><td>Switch</td><td>Fast loop response</td></tr>
	<tr><td>zone_double_eol_tamper</td><td>Switch</td><td>Double EOL tamper</td></tr>
	<tr><td>zone_type_trouble</td><td>Switch</td><td>Trouble</td></tr>
	<tr><td>zone_cross_zone</td><td>Switch</td><td>Cross zone</td></tr>
	<tr><td>zone_dialer_delay</td><td>Switch</td><td>Dialer delay</td></tr>
	<tr><td>zone_swinger_shutdown</td><td>Switch</td><td>Swinger shutdown</td></tr>
	<tr><td>zone_restorable</td><td>Switch</td><td>Restorable</td></tr>
	<tr><td>zone_listen_in</td><td>Switch</td><td>Listen in</td></tr>
	<tr><td>zone_faulted</td><td>Switch</td><td>Faulted (or delayed trip)</td></tr>
	<tr><td>zone_tampered</td><td>Switch</td><td>Tampered</td></tr>
	<tr><td>zone_trouble</td><td>Switch</td><td>Trouble</td></tr>
	<tr><td>zone_bypassed</td><td>Switch</td><td>Bypassed</td></tr>
	<tr><td>zone_inhibited</td><td>Switch</td><td>Inhibited (force armed)</td></tr>
	<tr><td>zone_low_battery</td><td>Switch</td><td>Low battery</td></tr>
	<tr><td>zone_loss_of_supervision</td><td>Switch</td><td>Loss of supervision</td></tr>
	<tr><td>zone_alarm_memory</td><td>Switch</td><td>Alarm memory</td></tr>
	<tr><td>zone_bypass_memory</td><td>Switch</td><td>Bypass memory</td></tr>
	<tr><td>zone_bypass_toggle</td><td>Switch</td><td>Send Zone bypass</td></tr>
</table>

## Full Example

The following is an example of an item file (caddx.items):

```java
Group Caddx
Group CaddxPanel (Caddx)
Group CaddxPartitions (Caddx)
Group CaddxZones (Caddx)
Group CaddxKeypads (Caddx)

/* Groups By Device Type */
Group:Contact:OR(OPEN, CLOSED) CaddxDoorWindow <door>
Group:Contact:OR(OPEN, CLOSED) CaddxMotion <motionDetector>
Group:Contact:OR(OPEN, CLOSED) CaddxSmoke <smokeDetector>

/* Caddx Alarm Items */
Switch BRIDGE_CONNECTION {channel="caddx:bridge:MyBridgeName:bridge_reset"}
String SEND_Caddx_ALARM_COMMAND "Send a Caddx Alarm Command" {channel="caddx:bridge:MyBridgeName:send_command"}

/* Caddx Alarm Panel Items */
String PANEL_MESSAGE "Panel Message: [%s]" (CaddxPanel) {channel="caddx:panel:MyBridgeName:panel:panel_message"}
Number PANEL_COMMAND "Panel Commands" (CaddxPanel) {channel="caddx:panel:MyBridgeName:panel:panel_command"}
String PANEL_SYSTEM_ERROR "Panel System Error: [%s]" (CaddxPanel) {channel="caddx:panel:MyBridgeName:panel:panel_system_error"}

String PANEL_FIRMWARE_VERSION "Firmware version: [%s]" <"shieldGreen"> (CaddxPanelPanel) {channel="caddx:panel:MyBridgeName:panel:panel_firmware_version"} 
Switch PANEL_INTERFACE_CONFIGURATION_MESSAGE (CaddxPanelPanel) Interface Configuration Message  {channel="caddx:panel:MyBridgeName:panel:panel_interface_configuration_message"}
panel_zone_status_message Switch Zone Status Message 
panel_zones_snapshot_message Switch Zones Snapshot Message 
panel_partition_status_message Switch Partition Status Message 
panel_partitions_snapshot_message Switch Partitions Snapshot Message 
panel_system_status_message Switch System Status Message 
panel_x10_message_received Switch X-10 Message Received 
panel_log_event_message Switch Log Event Message 
panel_keypad_message_received Switch Keypad Message Received 
panel_interface_configuration_request Switch Interface Configuration Request 
panel_zone_name_request Switch Zone Name Request 
panel_zone_status_request Switch Zone Status Request 
panel_zones_snapshot_request Switch Zones Snapshot Request 
panel_partition_status_request Switch Partition Status Request 
panel_partitions_snapshot_request Switch Partitions Snapshot Request 
panel_system_status_request Switch System Status Request 
panel_send_x10_message Switch Send X-10 Message 
panel_log_event_request Switch Log Event Request 
panel_send_keypad_text_message Switch Send Keypad Text Message 
panel_keypad_terminal_mode_request Switch Keypad Terminal Mode Request 
panel_program_data_request Switch Program Data Request 
panel_program_data_command Switch Program Data Command 
panel_user_information_request_with_pin Switch User Information Request with PIN 
panel_user_information_request_without_pin Switch User Information Request without PIN 
panel_set_user_code_command_with_pin Switch Set User Code Command with PIN 
panel_set_user_code_command_without_pin Switch Set User Code Command without PIN 
panel_set_user_authorization_command_with_pin Switch Set User Authorization Command with PIN 
panel_set_user_authorization_command_without_pin Switch Set User Authorization Command without PIN 
panel_store_communication_event_command Switch Store Communication Event Command 
panel_set_clock_calendar_command Switch Set Clock / Calendar Command 
panel_primary_keypad_function_with_pin Switch Primary Keypad Function with PIN 
panel_primary_keypad_function_without_pin Switch Primary Keypad Function without PIN 
panel_secondary_keypad_function Switch Secondary Keypad Function 
panel_zone_bypass_toggle Switch Zone Bypass Toggle 
partition_bypass_code_required Switch Bypass code required 
partition_fire_trouble Switch Fire trouble 
partition_fire Switch Fire 
partition_pulsing_buzzer Switch Pulsing Buzzer 
partition_tlm_fault_memory Switch TLM fault memory 
partition_armed Switch Armed 
partition_instant Switch Instant 
partition_previous_alarm Switch Previous Alarm 
partition_siren_on Switch Siren on 
partition_steady_siren_on Switch Steady siren on 
partition_alarm_memory Switch Alarm memory 
partition_tamper Switch Tamper 
partition_cancel_command_entered Switch Cancel command entered 
partition_code_entered Switch Code entered 
partition_cancel_pending Switch Cancel pending 
partition_silent_exit_enabled Switch Silent exit enabled 
partition_entryguard Switch Entryguard (stay mode) 
partition_chime_mode_on Switch Chime mode on 
partition_entry Switch Entry 
partition_delay_expiration_warning Switch Delay expiration warning 
partition_exit1 Switch Exit1 
partition_exit2 Switch Exit2 
partition_led_extinguish Switch LED extinguish 
partition_cross_timing Switch Cross timing 
partition_recent_closing_being_timed Switch Recent closing being timed 
partition_exit_error_triggered Switch Exit error triggered 
partition_auto_home_inhibited Switch Auto home inhibited 
partition_sensor_low_battery Switch Sensor low battery 
partition_sensor_lost_supervision Switch Sensor lost supervision 
partition_zone_bypassed Switch Zone bypassed 
partition_force_arm_triggered_by_auto_arm Switch Force arm triggered by auto arm 
partition_ready_to_arm Switch Ready to arm 
partition_ready_to_force_arm Switch Ready to force arm 
partition_valid_pin_accepted Switch Valid PIN accepted 
partition_chime_on Switch Chime on (sounding) 
partition_error_beep Switch Error beep (triple beep) 
partition_tone_on Switch Tone on (activation tone) 
partition_entry1 Switch Entry 1 
