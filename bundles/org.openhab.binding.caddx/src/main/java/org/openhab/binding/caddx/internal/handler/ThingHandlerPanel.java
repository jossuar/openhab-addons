/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.util.HexUtils;
import org.openhab.binding.caddx.internal.CaddxBindingConstants;
import org.openhab.binding.caddx.internal.CaddxEvent;
import org.openhab.binding.caddx.internal.CaddxMessage;
import org.openhab.binding.caddx.internal.CaddxMessageType;
import org.openhab.binding.caddx.internal.CaddxProperty;
import org.openhab.binding.caddx.internal.action.CaddxPanelActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a class for handling a Panel type Thing.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class ThingHandlerPanel extends CaddxBaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(ThingHandlerPanel.class);
    private @Nullable HashMap<String, String> panelLogMessagesMap = null;
    private @Nullable String communicatorStackPointer = null;
    private @Nullable String programData = null;

    public ThingHandlerPanel(Thing thing) {
        super(thing, CaddxThingType.PANEL);
    }

    @Override
    public void updateChannel(ChannelUID channelUID, String data) {
        if (channelUID.getId().equals(CaddxBindingConstants.PANEL_FIRMWARE_VERSION)
                || channelUID.getId().startsWith("panel_log_message_")) {
            updateState(channelUID, new StringType(data));
        } else {
            // All Panel channels are OnOffType
            OnOffType onOffType;

            onOffType = ("true".equals(data)) ? OnOffType.ON : OnOffType.OFF;
            updateState(channelUID, onOffType);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("handleCommand(): Command Received - {} {}.", channelUID, command);

        String cmd = null;
        String data = null;
        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        if (command instanceof RefreshType) {
            if (CaddxBindingConstants.PANEL_FIRMWARE_VERSION.equals(channelUID.getId())) {
                cmd = CaddxBindingConstants.PANEL_INTERFACE_CONFIGURATION_REQUEST;
                data = "";
            } else if (CaddxBindingConstants.PANEL_LOG_MESSAGE_N_0.equals(channelUID.getId())) {
                cmd = CaddxBindingConstants.PANEL_SYSTEM_STATUS_REQUEST;
                data = "";
            } else {
                return;
            }

            bridgeHandler.sendCommand(cmd, data);
        } else {
            logger.debug("Unknown command {}", command);
        }
    }

    @Override
    public void caddxEventReceived(CaddxEvent event, Thing thing) {
        logger.trace("caddxEventReceived(): Event Received - {}.", event);

        if (getThing().equals(thing)) {
            CaddxMessage message = event.getCaddxMessage();
            CaddxMessageType mt = message.getCaddxMessageType();
            ChannelUID channelUID = null;

            // Log event messages have special handling
            if (CaddxMessageType.PROGRAM_DATA_REPLY.equals(mt)) {
                handleProgramDataReply(message);
            } else if (CaddxMessageType.SYSTEM_STATUS_MESSAGE.equals(mt)) {
                handleSystemStatusMessage(message);
            } else if (CaddxMessageType.LOG_EVENT_MESSAGE.equals(mt)) {
                handleLogEventMessage(message);
            } else {
                for (CaddxProperty p : mt.properties) {
                    if (!p.getId().isEmpty()) {
                        String value = message.getPropertyById(p.getId());
                        channelUID = new ChannelUID(getThing().getUID(), p.getId());
                        updateChannel(channelUID, value);
                    }
                }
            }

            updateStatus(ThingStatus.ONLINE);
        }
    }

    private boolean buildBinaryString(int segmentOffset, int locationLength, byte[] dataBytes, StringBuilder sb) {
        for (int i = segmentOffset * 8; i < Math.min(locationLength, 8); i++) {
            for (int j = 7; j >= 0; j--) {
                if ((dataBytes[i] & 1 << j) != 0) {
                    sb.append("1-");
                } else {
                    sb.append("0-");
                }
            }
            sb.append("--");
        }

        return (segmentOffset * 8) + 8 < locationLength;
    }

    private boolean buildHexadecimalString(int segmentOffset, int segmentSize, int locationLength, byte[] dataBytes,
            StringBuilder sb) {
        boolean moreData = false;

        if (segmentSize == 1) { // nibbles
            for (int i = 0; i < Math.min(locationLength - (segmentOffset * 2 * 8), 16); i++) {
                if (i % 2 != 0) {
                    sb.append(new String(new byte[] { HexUtils.byteToHex(dataBytes[i / 2])[0] }));
                } else {
                    sb.append(new String(new byte[] { HexUtils.byteToHex(dataBytes[i / 2])[1] }));
                }
            }
            moreData = (segmentOffset * 16) + 16 < locationLength;
        } else { // bytes
            for (int i = 0; i < Math.min(locationLength - segmentOffset * 8, 8); i++) {
                sb.append(new String(new byte[] { HexUtils.byteToHex(dataBytes[i])[0] }));
                sb.append(" ");
            }
            moreData = (segmentOffset * 8) + 8 < locationLength;
        }

        return moreData;
    }

    /*
     * Gets the data at a specific location of the panel
     */
    private void handleProgramDataReply(CaddxMessage message) {
        logger.debug("handleProgramDataReply {}", message);

        int bussAddress = Integer.valueOf(message.getPropertyById("bussAddress"));
        int logicalLocationUpper = Integer.valueOf(message.getPropertyById("logicalLocationUpper"));
        int logicalLocationLower = Integer.valueOf(message.getPropertyById("logicalLocationLower"));

        int segmentOffset = Integer.valueOf(message.getPropertyById("panel_segment_offset"));
        int segmentSize = Integer.valueOf(message.getPropertyById("panel_segment_size"));
        int locationLength = Integer.valueOf(message.getPropertyById("panel_location_length"));
        int dataType = Integer.valueOf(message.getPropertyById("panel_data_type"));
        byte[] dataBytes = { Integer.valueOf(message.getPropertyById("panel_data_byte_0")).byteValue(),
                Integer.valueOf(message.getPropertyById("panel_data_byte_1")).byteValue(),
                Integer.valueOf(message.getPropertyById("panel_data_byte_2")).byteValue(),
                Integer.valueOf(message.getPropertyById("panel_data_byte_3")).byteValue(),
                Integer.valueOf(message.getPropertyById("panel_data_byte_4")).byteValue(),
                Integer.valueOf(message.getPropertyById("panel_data_byte_5")).byteValue(),
                Integer.valueOf(message.getPropertyById("panel_data_byte_6")).byteValue(),
                Integer.valueOf(message.getPropertyById("panel_data_byte_7")).byteValue() };

        StringBuilder sb = new StringBuilder();
        logger.debug("programData: {}", programData);
        sb.append(programData);

        logger.debug("ba: {}, lu: {}, ll: {}, so: {}, ss: {}, ln: {}, dt: {}, db: {}", bussAddress,
                logicalLocationUpper, logicalLocationLower, segmentOffset, segmentSize, locationLength, dataType,
                dataBytes);

        boolean moreData = false;
        switch (dataType) {
            case 0: // Binary
                moreData = buildBinaryString(segmentOffset, locationLength, dataBytes, sb);
                break;
            case 1: // Decimal
                moreData = buildHexadecimalString(segmentOffset, segmentSize, locationLength, dataBytes, sb);
                break;
            case 2: // Hexadecimal
                moreData = buildHexadecimalString(segmentOffset, segmentSize, locationLength, dataBytes, sb);
                break;
            case 3: // Ascii
                break;
            default:
                return;
        }

        if (moreData) {
            programData = sb.toString();
            logger.debug("First part of reply: {}", programData);

            CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
            if (bridgeHandler != null) {
                logicalLocationUpper = logicalLocationUpper | 0x40; // Turn on Bit 6 to get the next set of data

                bridgeHandler.sendCommand(CaddxBindingConstants.PANEL_PROGRAM_DATA_REQUEST,
                        bussAddress + "," + logicalLocationUpper + "," + logicalLocationLower);
            }
        } else {
            logger.debug("Full reply: {}", sb.toString());

            ChannelUID channelUID = new ChannelUID(getThing().getUID(), "panel_firmware_version");
            updateChannel(channelUID, sb.toString());
            logger.info("=Value {}", sb.toString());

            programData = null;
        }
    }

    /*
     * Gets the pointer into the panel's log messages ring buffer
     * and sends the command for the retrieval of the last event_message
     */
    private void handleSystemStatusMessage(CaddxMessage message) {
        // Get the bridge handler
        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        String pointer = message.getPropertyById("panel_communicator_stack_pointer");
        communicatorStackPointer = pointer;

        // build map of log message channels to event numbers
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(pointer, CaddxBindingConstants.PANEL_LOG_MESSAGE_N_0);
        bridgeHandler.sendCommand(CaddxBindingConstants.PANEL_LOG_EVENT_REQUEST, pointer);
        panelLogMessagesMap = map;
    }

    /*
     * This function handles the panel log messages.
     * If the received event_number matches our communication stack pointer then this is the last panel message. The
     * channel gets updated and the required log message requests are generated for the update of the other log message
     * channels
     */
    private void handleLogEventMessage(CaddxMessage message) {
        // Get the bridge handler
        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        String eventNumberString = message.getPropertyById("panel_log_event_number");
        String eventSizeString = message.getPropertyById("panel_log_event_size");

        // build the message
        LogEventMessage logEventMessage = new LogEventMessage(message);

        logger.trace("Log_event: {}", logEventMessage);

        // get the channel id from the map
        HashMap<String, String> logMap = panelLogMessagesMap;
        if (logMap != null && logMap.containsKey(eventNumberString)) {
            String id = logMap.get(eventNumberString);
            ChannelUID channelUID = new ChannelUID(getThing().getUID(), id);
            updateChannel(channelUID, logEventMessage.toString());
        }

        if (communicatorStackPointer != null && eventNumberString.equals(communicatorStackPointer)) {
            HashMap<String, String> map = new HashMap<String, String>();

            int eventNumber = Integer.parseInt(eventNumberString);
            int eventSize = Integer.parseInt(eventSizeString);

            // Retrieve at maximum the 10 last log messages from the panel
            int messagesToRetrieve = Math.min(eventSize, 10);
            for (int i = 1; i < messagesToRetrieve; i++) {
                eventNumber--;
                if (eventNumber < 0) {
                    eventNumber = eventSize;
                }

                map.put(Integer.toString(eventNumber), "panel_log_message_n_" + i);
                bridgeHandler.sendCommand(CaddxBindingConstants.PANEL_LOG_EVENT_REQUEST, Integer.toString(eventNumber));
            }

            communicatorStackPointer = null;
            panelLogMessagesMap = map;
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(CaddxPanelActions.class);
    }

    private void sendPrimaryCommand(String pin, String function) {
        String cmd = CaddxBindingConstants.PARTITION_PRIMARY_COMMAND_WITH_PIN;

        // Build the data
        StringBuilder sb = new StringBuilder();
        sb.append("0x").append(pin.charAt(1)).append(pin.charAt(0)).append(",0x").append(pin.charAt(3))
                .append(pin.charAt(2)).append(",0x").append(pin.charAt(5)).append(pin.charAt(4)).append(",")
                .append(function).append(",").append("255");

        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }
        bridgeHandler.sendCommand(cmd, sb.toString());
    }

    private void sendSecondaryCommand(String function) {
        String cmd = CaddxBindingConstants.PARTITION_SECONDARY_COMMAND;

        // Build the data
        StringBuilder sb = new StringBuilder();
        sb.append(function).append(",").append("255");

        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }
        bridgeHandler.sendCommand(cmd, sb.toString());
    }

    public void turnOffAnySounderOrAlarm(String pin) {
        sendPrimaryCommand(pin, "0");
    }

    public void disarm(String pin) {
        sendPrimaryCommand(pin, "1");
    }

    public void armInAwayMode(String pin) {
        sendPrimaryCommand(pin, "2");
    }

    public void armInStayMode(String pin) {
        sendPrimaryCommand(pin, "3");
    }

    public void cancel(String pin) {
        sendPrimaryCommand(pin, "4");
    }

    public void initiateAutoArm(String pin) {
        sendPrimaryCommand(pin, "5");
    }

    public void startWalkTestMode(String pin) {
        sendPrimaryCommand(pin, "6");
    }

    public void stopWalkTestMode(String pin) {
        sendPrimaryCommand(pin, "7");
    }

    public void stay() {
        sendSecondaryCommand("0");
    }

    public void chime() {
        sendSecondaryCommand("1");
    }

    public void exit() {
        sendSecondaryCommand("2");
    }

    public void bypassInteriors() {
        sendSecondaryCommand("3");
    }

    public void firePanic() {
        sendSecondaryCommand("4");
    }

    public void medicalPanic() {
        sendSecondaryCommand("5");
    }

    public void policePanic() {
        sendSecondaryCommand("6");
    }

    public void smokeDetectorReset() {
        sendSecondaryCommand("7");
    }

    public void autoCallbackDownload() {
        sendSecondaryCommand("8");
    }

    public void manualPickupDownload() {
        sendSecondaryCommand("9");
    }

    public void enableSilentExit() {
        sendSecondaryCommand("10");
    }

    public void performTest() {
        sendSecondaryCommand("11");
    }

    public void groupBypass() {
        sendSecondaryCommand("12");
    }

    public void auxiliaryFunction1() {
        sendSecondaryCommand("13");
    }

    public void auxiliaryFunction2() {
        sendSecondaryCommand("14");
    }

    public void startKeypadSounder() {
        sendSecondaryCommand("15");
    }

    public void requestProgramData(int deviceAddress, int logicalLocation) {
        String cmd = CaddxBindingConstants.PANEL_PROGRAM_DATA_REQUEST;

        CaddxBridgeHandler bridgeHandler = getCaddxBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Build the data for the 1st command
        int lowerByte = logicalLocation & 0xff;
        int upperByte = (logicalLocation & 0xf00) >> 8;

        StringBuilder sb = new StringBuilder();
        sb.append(deviceAddress).append(",").append(upperByte).append(",").append(lowerByte);

        programData = "";
        bridgeHandler.sendCommand(cmd, sb.toString());
    }
}
