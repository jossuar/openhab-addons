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

import static org.openhab.binding.caddx.internal.CaddxBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.TooManyListenersException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.caddx.internal.CaddxBindingConstants;
import org.openhab.binding.caddx.internal.CaddxCommunicator;
import org.openhab.binding.caddx.internal.CaddxCommunicator.SecurityPanelListener;
import org.openhab.binding.caddx.internal.CaddxEvent;
import org.openhab.binding.caddx.internal.CaddxMessage;
import org.openhab.binding.caddx.internal.CaddxMessage.Source;
import org.openhab.binding.caddx.internal.CaddxProtocol;
import org.openhab.binding.caddx.internal.config.CaddxBridgeConfiguration;
import org.openhab.binding.caddx.internal.config.CaddxKeypadConfiguration;
import org.openhab.binding.caddx.internal.config.CaddxPartitionConfiguration;
import org.openhab.binding.caddx.internal.config.CaddxZoneConfiguration;
import org.openhab.binding.caddx.internal.discovery.CaddxDiscoveryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

/**
 * The bridge handler for the Caddx RS232 Serial interface.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class CaddxBridgeHandler extends BaseBridgeHandler implements SecurityPanelListener {
    private final Logger logger = LoggerFactory.getLogger(CaddxBridgeHandler.class);

    static final byte[] DISCOVERY_PARTITION_STATUS_REQUEST_0 = { 0x26, 0x00 };
    static final byte[] DISCOVERY_ZONES_SNAPSHOT_REQUEST_00 = { 0x25, 0x00 };
    static final byte[] DISCOVERY_ZONES_SNAPSHOT_REQUEST_10 = { 0x25, 0x10 };
    static final byte[] DISCOVERY_ZONES_SNAPSHOT_REQUEST_20 = { 0x25, 0x20 };
    static final byte[] DISCOVERY_ZONES_SNAPSHOT_REQUEST_30 = { 0x25, 0x30 };
    static final byte[] DISCOVERY_PARTITIONS_SNAPSHOT_REQUEST = { 0x27 };
    static final byte[] DISCOVERY_SYSTEM_STATUS_REQUEST = { 0x28 };

    private @Nullable CaddxDiscoveryService discoveryService = null;

    public @Nullable CaddxDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    public void setDiscoveryService(CaddxDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    /**
     * Constructor.
     *
     * @param bridge
     */
    public CaddxBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    private CaddxProtocol protocol = CaddxProtocol.Binary;
    private String serialPortName = "";
    private int baudRate;
    private @Nullable CaddxCommunicator communicator = null;

    private void init() {
        CaddxBridgeConfiguration configuration = getConfigAs(CaddxBridgeConfiguration.class);

        protocol = configuration.getProtocol();
        serialPortName = configuration.getSerialPort();
        baudRate = configuration.getBaudrate().intValue();
        updateStatus(ThingStatus.OFFLINE);

        // create & start panel interface
        logger.info("starting interface at port {} with baudrate {}", serialPortName, baudRate);
        try {
            communicator = new CaddxCommunicator(protocol, serialPortName, baudRate);
        } catch (UnsupportedCommOperationException | NoSuchPortException | PortInUseException | IOException
                | TooManyListenersException e) {
            logger.warn("Cannot initialize Communication.", e);
            throw new IllegalArgumentException();
        }

        CaddxCommunicator myCommunicator = communicator;
        if (myCommunicator != null) {
            myCommunicator.addListener(this);

            // Send discovery commands for the things
            myCommunicator.transmit(new CaddxMessage(DISCOVERY_ZONES_SNAPSHOT_REQUEST_00, false));
            myCommunicator.transmit(new CaddxMessage(DISCOVERY_ZONES_SNAPSHOT_REQUEST_10, false));
            myCommunicator.transmit(new CaddxMessage(DISCOVERY_ZONES_SNAPSHOT_REQUEST_20, false));
            myCommunicator.transmit(new CaddxMessage(DISCOVERY_PARTITION_STATUS_REQUEST_0, false));
            myCommunicator.transmit(new CaddxMessage(DISCOVERY_PARTITIONS_SNAPSHOT_REQUEST, false));
            myCommunicator.transmit(new CaddxMessage(DISCOVERY_SYSTEM_STATUS_REQUEST, false));
        }
        // list all channels
        if (logger.isTraceEnabled()) {
            logger.trace("list all " + getThing().getChannels().size() + " channels:");
            for (Channel c : getThing().getChannels()) {
                logger.trace(String.format("Channel Type %s UID %s", c.getChannelTypeUID(), c.getUID()));
            }
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing the Bridge handler.");

        init();
    }

    @Override
    public void dispose() {
        CaddxCommunicator n = communicator;
        if (n != null) {
            n.stop();
            n = null;
        }

        if (discoveryService != null) {
            unregisterDiscoveryService();
        }

        super.dispose();
    }

    public @Nullable Thing findThing(CaddxThingType caddxThingType, @Nullable Integer partition, @Nullable Integer zone,
            @Nullable Integer keypad) {
        List<Thing> things = getThing().getThings();

        Thing thing = null;

        for (Thing t : things) {
            try {
                Configuration config = t.getConfiguration();
                CaddxBaseThingHandler handler = (CaddxBaseThingHandler) t.getHandler();

                if (handler != null) {
                    CaddxThingType handlerCaddxThingType = handler.getCaddxThingType();

                    if (handlerCaddxThingType.equals(caddxThingType)) {
                        switch (handlerCaddxThingType) {
                            case PANEL:
                                thing = t;
                                return thing;
                            case KEYPAD:
                                BigDecimal keypadAddress = (BigDecimal) config
                                        .get(CaddxKeypadConfiguration.KEYPAD_ADDRESS);
                                if (keypad == keypadAddress.intValue()) {
                                    thing = t;
                                    logger.trace("findThing(): Thing Found - {}", handlerCaddxThingType);
                                    return thing;
                                }
                                break;
                            case PARTITION:
                                BigDecimal partitionNumber = (BigDecimal) config
                                        .get(CaddxPartitionConfiguration.PARTITION_NUMBER);
                                if (partition == partitionNumber.intValue()) {
                                    thing = t;
                                    return thing;
                                }
                                break;
                            case ZONE:
                                BigDecimal zoneNumber = (BigDecimal) config.get(CaddxZoneConfiguration.ZONE_NUMBER);
                                if (zone == zoneNumber.intValue()) {
                                    thing = t;
                                    return thing;
                                }
                                break;
                            default:
                                break;
                        }
                    }

                }
            } catch (Exception e) {
                logger.error("findThing(): Error Searching Thing - {} ", e.getMessage(), e);
            }
        }

        return thing;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(), channelUID: {}, command: {}", channelUID, command);

        switch (channelUID.getId()) {
            case BRIDGE_RESET:
                if (command == OnOffType.ON) {
                    CaddxCommunicator n = communicator;
                    if (n != null) {
                        n.stop();
                        n = null;
                        updateStatus(ThingStatus.OFFLINE);
                    }
                } else if (command == OnOffType.OFF) {
                    init();
                    updateStatus(ThingStatus.ONLINE);
                }
                break;
            case SEND_COMMAND:
                if (!command.toString().isEmpty()) {
                    String[] tokens = command.toString().split(",");

                    String cmd = tokens[0];
                    String data = "";
                    if (tokens.length > 1) {
                        data = tokens[1];
                    }

                    sendCommand(cmd, data);

                    updateState(channelUID, new StringType(""));
                }
                break;
            default:
                break;
        }
    }

    /**
     * Sends a command to the panel
     *
     * @param command The command to be send
     * @param data The associated command data
     */
    public boolean sendCommand(String command, String data) {
        logger.debug("sendCommand(): Attempting to send Command: command - {} - data: {}", command, data);

        CaddxMessage msg = null;

        if (CaddxBindingConstants.ZONE_BYPASS_TOGGLE.equals(command)) {
            msg = CaddxMessage.buildZoneBypassToggle(data);
        } else if (CaddxBindingConstants.ZONE_STATUS_REQUEST.equals(command)) {
            msg = CaddxMessage.buildZoneStatusRequest(data);
        } else if (CaddxBindingConstants.ZONE_NAME_REQUEST.equals(command)) {
            msg = CaddxMessage.buildZoneNameRequest(data);
        } else if (CaddxBindingConstants.PARTITION_STATUS_REQUEST.equals(command)) {
            msg = CaddxMessage.buildPartitionStatusRequest(data);
        } else if (CaddxBindingConstants.PARTITION_PRIMARY_COMMAND.equals(command)) {
            msg = CaddxMessage.buildPartitionPrimaryCommand(data);
        } else if (CaddxBindingConstants.PARTITION_SECONDARY_COMMAND.equals(command)) {
            msg = CaddxMessage.buildPartitionSecondaryCommand(data);
        } else if (CaddxBindingConstants.PANEL_INTERFACE_CONFIGURATION_REQUEST.equals(command)) {
            msg = CaddxMessage.buildInterfaceConfigurationRequest(data);
        } else if (CaddxBindingConstants.PANEL_LOG_EVENT_REQUEST.equals(command)) {
            msg = CaddxMessage.buildLogEventRequest(data);
        } else {
            return false;
        }

        CaddxCommunicator n = communicator;
        if (n != null) {
            n.transmit(msg);
        }

        return true;
    }

    /**
     * Register the Discovery Service.
     *
     * @param discoveryService
     */
    public void registerDiscoveryService(@Nullable CaddxDiscoveryService discoveryService) {
        if (discoveryService == null) {
            throw new IllegalArgumentException("registerDiscoveryService(): Illegal Argument. Not allowed to be Null!");
        }

        this.discoveryService = discoveryService;
        logger.trace("registerDiscoveryService(): Discovery Service Registered!");
    }

    /**
     * Unregister the Discovery Service.
     */
    public void unregisterDiscoveryService() {
        logger.trace("unregisterDiscoveryService(): Discovery Service Unregistered!");
        discoveryService = null;
    }

    @Override
    public void caddxMessage(CaddxCommunicator communicator, CaddxMessage caddxMessage) {
        Source source = caddxMessage.getSource();

        if (source != Source.None) {
            CaddxThingType caddxThingType = null;
            @Nullable
            Integer partition = null;
            @Nullable
            Integer zone = null;
            @Nullable
            Integer keypad = null;

            switch (source) {
                case Panel:
                    caddxThingType = CaddxThingType.PANEL;
                    break;
                case Partition:
                    caddxThingType = CaddxThingType.PARTITION;
                    partition = Integer.parseInt(caddxMessage.getPropertyById("partition_number")) + 1;
                    break;
                case Zone:
                    caddxThingType = CaddxThingType.ZONE;
                    zone = Integer.parseInt(caddxMessage.getPropertyById("zone_number")) + 1;
                    break;
                case Keypad:
                    caddxThingType = CaddxThingType.KEYPAD;
                    keypad = Integer.parseInt(caddxMessage.getPropertyById("keypad_address"));
                    break;
                default:
                    throw new IllegalArgumentException("Source has illegal value");
            }

            CaddxEvent event = new CaddxEvent(caddxMessage, partition, zone, keypad);

            // Find the thing
            Thing thing = findThing(caddxThingType, partition, zone, keypad);
            if (thing != null) {
                CaddxBaseThingHandler thingHandler = (CaddxBaseThingHandler) thing.getHandler();
                if (thingHandler != null) {
                    thingHandler.caddxEventReceived(event, thing);
                }
            } else {
                CaddxDiscoveryService d = getDiscoveryService();
                if (d != null) {
                    d.addThing(getThing(), caddxThingType, event);
                }
            }

            if (getDiscoveryService() != null) {
                switch (caddxMessage.getCaddxMessageType()) {
                    case Partitions_Snapshot_Message:
                        for (int i = 1; i <= 8; i++) {
                            if (caddxMessage.getPropertyById("partition_" + Integer.toString(i) + "_valid") == "true") {
                                thing = findThing(CaddxThingType.PARTITION, i, null, null);
                                if (thing != null) {
                                    continue;
                                }

                                event = new CaddxEvent(caddxMessage, i, null, null);
                                CaddxDiscoveryService d = getDiscoveryService();
                                if (d != null) {
                                    d.addThing(getThing(), CaddxThingType.PARTITION, event);
                                }
                            }
                        }
                        break;
                    case Zones_Snapshot_Message:
                        int zoneOffset = Integer.parseInt(caddxMessage.getPropertyById("zone_offset"));
                        for (int i = 1; i <= 16; i++) {
                            if (caddxMessage.getPropertyById("zone_" + Integer.toString(i) + "_trouble")
                                    .equals("false")) {
                                thing = findThing(CaddxThingType.ZONE, null, zoneOffset + i, null);
                                if (thing != null) {
                                    continue;
                                }

                                event = new CaddxEvent(caddxMessage, null, zoneOffset + i, null);
                                CaddxDiscoveryService d = getDiscoveryService();
                                if (d != null) {
                                    d.addThing(getThing(), CaddxThingType.ZONE, event);
                                }
                            }
                        }
                        break;
                    case Zone_Status_Message:
                        break;
                    case Zone_Name_Message:
                        break;
                    default:
                        break;
                }
            }
        }

        updateStatus(ThingStatus.ONLINE);
    }
}
