/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.risco.internal.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.RiscoCommunicator;
import org.openhab.binding.risco.internal.RiscoCommunicator.RiscoPanelListener;
import org.openhab.binding.risco.internal.config.RiscoBridgeConfiguration;
import org.openhab.binding.risco.internal.config.RiscoBusExpanderConfiguration;
import org.openhab.binding.risco.internal.config.RiscoKeyfobConfiguration;
import org.openhab.binding.risco.internal.config.RiscoKeypadConfiguration;
import org.openhab.binding.risco.internal.config.RiscoOutputConfiguration;
import org.openhab.binding.risco.internal.config.RiscoOutputExpanderConfiguration;
import org.openhab.binding.risco.internal.config.RiscoPartitionConfiguration;
import org.openhab.binding.risco.internal.config.RiscoPowerSupplyConfiguration;
import org.openhab.binding.risco.internal.config.RiscoProximityReaderConfiguration;
import org.openhab.binding.risco.internal.config.RiscoSounderConfiguration;
import org.openhab.binding.risco.internal.config.RiscoWirelessModuleConfiguration;
import org.openhab.binding.risco.internal.config.RiscoZoneConfiguration;
import org.openhab.binding.risco.internal.config.RiscoZoneExpanderConfiguration;
import org.openhab.binding.risco.internal.discovery.RiscoDiscoveryService;
import org.openhab.binding.risco.internal.handler.thing.RiscoBusExpanderHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoKeyfobHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoKeypadHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoOutputExpanderHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoOutputHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoPanelHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoPartitionHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoPowerSupplyHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoProximityReaderHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoSounderHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoVoiceModuleHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoWirelessModuleHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoZoneExpanderHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoZoneHandler;
import org.openhab.binding.risco.internal.protocol.DiscoveryInfo;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoThing;
import org.openhab.binding.risco.internal.protocol.RiscoThingType;
import org.openhab.binding.risco.internal.protocol.message.connection.Local;
import org.openhab.binding.risco.internal.protocol.message.connection.Remote;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoBridgeHandler extends BaseBridgeHandler implements RiscoPanelListener {
    private final Logger logger = LoggerFactory.getLogger(RiscoBridgeHandler.class);

    // Things served by the bridge
    private Map<String, Thing> thingGeneralMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingBusExpanderMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingKeyfobMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingKeypadMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingOutputMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingOutputExpanderMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingPartitionMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingPowerSupplyMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingProximityReaderMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingSounderMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingWirelessModuleMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingZoneMap = new ConcurrentHashMap<>();
    private Map<Integer, Thing> thingZoneExpanderMap = new ConcurrentHashMap<>();

    // Communication
    private @Nullable RiscoCommunicator communicator = null;
    private @Nullable RiscoDiscoveryService discoveryService = null;

    public RiscoBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public synchronized void initialize() {
        updateStatus(ThingStatus.UNKNOWN);

        RiscoBridgeConfiguration configuration = getConfigAs(RiscoBridgeConfiguration.class);

        String hostname = configuration.getHostname();
        if (hostname == null || hostname.trim().isEmpty()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Set an IP address in the thing configuration.");

            return;
        }

        int port = configuration.getPort();
        updateStatus(ThingStatus.OFFLINE);

        // create & start panel interface
        logger.debug("Starting interface with host {} at port {}", hostname, port);

        try {
            RiscoCommunicator communicator = new RiscoCommunicator(getThing().getUID().getAsString(), hostname, port,
                    configuration.getId(), configuration.getEncoding(), configuration.getPassword(), scheduler);

            // Initialize the communication with the panel
            communicator.sendPlainAndWait(Remote.getReadCommand(configuration.getPassword()));
            communicator.sendPlainAndWait(Local.getReadCommand());

            communicator.addListener(this);
            this.communicator = communicator;

            updateStatus(ThingStatus.ONLINE);

            notifyAll();
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication cannot be initialized. " + e.toString());

            return;
        }
    }

    @Override
    public void dispose() {
        RiscoCommunicator comm = communicator;
        if (comm != null) {
            comm.stop();
            comm = null;
        }

        super.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.trace("handleCommand(), channelUID: {}, command: {}", channelUID, command);

        switch (channelUID.getId()) {
            default:
                logger.debug("Unknown command {}", command);
                break;
        }
    }

    public void restart() {
        // Stop the currently running communicator
        RiscoCommunicator comm = communicator;
        if (comm != null) {
            comm.stop();
            comm = null;
        }

        // Initialize again
        initialize();
    }

    @Override
    public void handleRiscoMessage(RiscoMessage msg) {
        logger.debug("handleRiscoMessage: {}", msg);

        // Get the list of things
        Thing thing = null;

        for (RiscoThing rt : msg.getThings()) {
            logger.debug("handleRiscoMessage: {}-{}", rt.getRiscoThingType(), rt.getIndex());
            thing = findThing(rt.getRiscoThingType(), rt.getIndex());

            logger.debug("handleRiscoMessage: thing[{}]", thing);

            if (thing != null) {
                RiscoThingHandler thingHandler = (RiscoThingHandler) thing.getHandler();
                if (thingHandler != null) {
                    thingHandler.handleEvent(rt);
                }
            } else {
                RiscoDiscoveryService discoveryService = this.discoveryService;
                if (discoveryService != null) {
                    DiscoveryInfo di = mapInfo(rt.getRiscoThingType(), rt.getIndex());

                    discoveryService.addThing(getThing(), di.getThingUID(), di.getThingLabel(), di.getIndexProperty(),
                            di.getIndex());
                }
            }
        }
    }

    public @Nullable Thing findThing(RiscoThingType riscoThingType, @Nullable Integer index) {
        switch (riscoThingType) {
            case PANEL:
                return thingGeneralMap.get(RiscoBindingConstants.PANEL);
            case CELLULAR_ON_BUS:
                break;
            case VOICE_MODULE:
                return thingGeneralMap.get(RiscoBindingConstants.VOICE_MODULE);
            default:
                break;
        }
        if (index == null) {
            return null;
        }

        switch (riscoThingType) {
            case BUS_EXPANDER:
                return thingBusExpanderMap.get(Integer.valueOf(index));
            case KEYFOB:
                return thingKeyfobMap.get(Integer.valueOf(index));
            case KEYPAD:
                return thingKeypadMap.get(Integer.valueOf(index));
            case OUTPUT:
                return thingOutputMap.get(Integer.valueOf(index));
            case OUTPUT_EXPANDER:
                return thingOutputExpanderMap.get(Integer.valueOf(index));
            case PARTITION:
                return thingPartitionMap.get(Integer.valueOf(index));
            case POWER_SUPPLY:
                return thingPowerSupplyMap.get(Integer.valueOf(index));
            case PROXIMITY_READER:
                return thingProximityReaderMap.get(Integer.valueOf(index));
            case SOUNDER:
                return thingSounderMap.get(Integer.valueOf(index));
            case WIRELESS_MODULE:
                return thingWirelessModuleMap.get(Integer.valueOf(index));
            case ZONE:
                return thingZoneMap.get(Integer.valueOf(index));
            case ZONE_EXPANDER:
                return thingZoneExpanderMap.get(Integer.valueOf(index));
            default:
                break;
        }
        return null;
    }

    private DiscoveryInfo mapInfo(RiscoThingType type, @Nullable Integer index) {
        ThingTypeUID ttUID;
        String prefix;
        String label;
        String indexProperty;

        switch (type) {
            case BUS_EXPANDER:
                ttUID = RiscoBindingConstants.BUS_EXPANDER_THING_TYPE;
                prefix = RiscoBindingConstants.BUS_EXPANDER;
                label = "Bus Expander " + index;
                indexProperty = RiscoBusExpanderConfiguration.BUS_EXPANDER_NUMBER;
                break;
            case CELLULAR_ON_BUS:
                ttUID = RiscoBindingConstants.CELLULAR_ON_BUS_THING_TYPE;
                prefix = RiscoBindingConstants.CELLULAR_ON_BUS;
                label = "Cellular on Bus " + index;
                indexProperty = "cellularOnBusNumber";
                break;
            case KEYFOB:
                ttUID = RiscoBindingConstants.KEYFOB_THING_TYPE;
                prefix = RiscoBindingConstants.KEYFOB;
                label = "Keyfob " + index;
                indexProperty = RiscoKeyfobConfiguration.KEYFOB_NUMBER;
                break;
            case KEYPAD:
                ttUID = RiscoBindingConstants.KEYPAD_THING_TYPE;
                prefix = RiscoBindingConstants.KEYPAD;
                label = "Keypad " + index;
                indexProperty = RiscoKeypadConfiguration.KEYPAD_NUMBER;
                break;
            case OUTPUT:
                ttUID = RiscoBindingConstants.OUTPUT_THING_TYPE;
                prefix = RiscoBindingConstants.OUTPUT;
                label = "Output " + index;
                indexProperty = RiscoOutputConfiguration.OUTPUT_NUMBER;
                break;
            case OUTPUT_EXPANDER:
                ttUID = RiscoBindingConstants.OUTPUT_EXPANDER_THING_TYPE;
                prefix = RiscoBindingConstants.OUTPUT_EXPANDER;
                label = "Output Expander " + index;
                indexProperty = RiscoOutputExpanderConfiguration.OUTPUT_EXPANDER_NUMBER;
                break;
            case PARTITION:
                ttUID = RiscoBindingConstants.PARTITION_THING_TYPE;
                prefix = RiscoBindingConstants.PARTITION;
                label = "Partition " + index;
                indexProperty = RiscoPartitionConfiguration.PARTITION_NUMBER;
                break;
            case POWER_SUPPLY:
                ttUID = RiscoBindingConstants.POWER_SUPPLY_THING_TYPE;
                prefix = RiscoBindingConstants.POWER_SUPPLY;
                label = "Power Supply " + index;
                indexProperty = RiscoPowerSupplyConfiguration.POWER_SUPPLY_NUMBER;
                break;
            case PROXIMITY_READER:
                ttUID = RiscoBindingConstants.PROXIMITY_READER_THING_TYPE;
                prefix = RiscoBindingConstants.PROXIMITY_READER;
                label = "Proximity Reader " + index;
                indexProperty = RiscoProximityReaderConfiguration.PROXIMITY_READER_NUMBER;
                break;
            case SOUNDER:
                ttUID = RiscoBindingConstants.SOUNDER_THING_TYPE;
                prefix = RiscoBindingConstants.SIREN;
                label = "Sounder " + index;
                indexProperty = RiscoSounderConfiguration.SOUNDER_NUMBER;
                break;
            case PANEL:
                ttUID = RiscoBindingConstants.PANEL_THING_TYPE;
                prefix = RiscoBindingConstants.PANEL;
                label = "Panel";
                indexProperty = null;
                break;
            case VOICE_MODULE:
                ttUID = RiscoBindingConstants.VOICE_MODULE_THING_TYPE;
                prefix = RiscoBindingConstants.VOICE_MODULE;
                label = "Voice Module";
                indexProperty = null;
                break;
            case WIRELESS_MODULE:
                ttUID = RiscoBindingConstants.WIRELESS_MODULE_THING_TYPE;
                prefix = RiscoBindingConstants.WIRELESS_MODULE;
                label = "Wireless Module " + index;
                indexProperty = RiscoWirelessModuleConfiguration.WIRELESS_MODULE_NUMBER;
                break;
            case ZONE:
                ttUID = RiscoBindingConstants.ZONE_THING_TYPE;
                prefix = RiscoBindingConstants.ZONE;
                label = "Zone " + index;
                indexProperty = RiscoZoneConfiguration.ZONE_NUMBER;
                break;
            case ZONE_EXPANDER:
                ttUID = RiscoBindingConstants.ZONE_EXPANDER_THING_TYPE;
                prefix = RiscoBindingConstants.ZONE_EXPANDER;
                label = "Zone Expander " + index;
                indexProperty = RiscoZoneExpanderConfiguration.ZONE_EXPANDER_NUMBER;
                break;
            default:
                logger.debug("getThingUID: Missing enum case");
                throw new IllegalArgumentException("getThingUID: type is unknown. [" + type + "]");

        }

        ThingUID thingUID = new ThingUID(ttUID, getThing().getUID(), prefix + ((index == null) ? "" : index));
        DiscoveryInfo discoveryInfo = new DiscoveryInfo(thingUID, label, indexProperty, index);

        return discoveryInfo;
    }

    /**
     * Sends a command to the panel
     *
     * @param command The command to be send
     * @param data The associated command data
     */
    public synchronized boolean sendCommand(String command) {
        logger.trace("sendCommand(): Attempting to send Command: command - {}", command);

        // Wait until the communicator initializes
        while (communicator == null) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        RiscoCommunicator comm = communicator;
        if (comm != null) {
            comm.send(command);
        }

        return true;
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof RiscoPanelHandler) {
            thingGeneralMap.put(RiscoBindingConstants.PANEL, childThing);
        } else if (childHandler instanceof RiscoBusExpanderHandler) {
            RiscoBusExpanderHandler handler = (RiscoBusExpanderHandler) childHandler;
            thingBusExpanderMap.put(handler.getBusExpanderNumber(), childThing);
        } else if (childHandler instanceof RiscoKeyfobHandler) {
            RiscoKeyfobHandler handler = (RiscoKeyfobHandler) childHandler;
            thingKeyfobMap.put(handler.getKeyfobNumber(), childThing);
        } else if (childHandler instanceof RiscoKeypadHandler) {
            RiscoKeypadHandler handler = (RiscoKeypadHandler) childHandler;
            thingKeypadMap.put(handler.getKeypadNumber(), childThing);
        } else if (childHandler instanceof RiscoOutputHandler) {
            RiscoOutputHandler handler = (RiscoOutputHandler) childHandler;
            thingOutputMap.put(handler.getOutputNumber(), childThing);
        } else if (childHandler instanceof RiscoOutputExpanderHandler) {
            RiscoOutputExpanderHandler handler = (RiscoOutputExpanderHandler) childHandler;
            thingOutputExpanderMap.put(handler.getOutputExpanderNumber(), childThing);
        } else if (childHandler instanceof RiscoPartitionHandler) {
            RiscoPartitionHandler handler = (RiscoPartitionHandler) childHandler;
            thingPartitionMap.put(handler.getPartitionNumber(), childThing);
        } else if (childHandler instanceof RiscoPowerSupplyHandler) {
            RiscoPowerSupplyHandler handler = (RiscoPowerSupplyHandler) childHandler;
            thingPowerSupplyMap.put(handler.getPowerSupplyNumber(), childThing);
        } else if (childHandler instanceof RiscoProximityReaderHandler) {
            RiscoProximityReaderHandler handler = (RiscoProximityReaderHandler) childHandler;
            thingPartitionMap.put(handler.getProximityReaderNumber(), childThing);
        } else if (childHandler instanceof RiscoSounderHandler) {
            RiscoSounderHandler handler = (RiscoSounderHandler) childHandler;
            thingSounderMap.put(handler.getSounderNumber(), childThing);
        } else if (childHandler instanceof RiscoVoiceModuleHandler) {
            thingGeneralMap.put(RiscoBindingConstants.VOICE_MODULE, childThing);
        } else if (childHandler instanceof RiscoWirelessModuleHandler) {
            RiscoWirelessModuleHandler handler = (RiscoWirelessModuleHandler) childHandler;
            thingWirelessModuleMap.put(handler.getWirelessModuleNumber(), childThing);
        } else if (childHandler instanceof RiscoZoneHandler) {
            RiscoZoneHandler handler = (RiscoZoneHandler) childHandler;
            thingZoneMap.put(handler.getZoneNumber(), childThing);
        } else if (childHandler instanceof RiscoZoneExpanderHandler) {
            RiscoZoneExpanderHandler handler = (RiscoZoneExpanderHandler) childHandler;
            thingZoneExpanderMap.put(handler.getZoneExpanderNumber(), childThing);
        }

        super.childHandlerInitialized(childHandler, childThing);

        // refresh all channels
        /*
         * for (Channel c : childThing.getChannels()) {
         * childHandler.handleCommand(c.getUID(), RefreshType.REFRESH);
         * }
         */
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof RiscoPanelHandler) {
            thingGeneralMap.remove(RiscoBindingConstants.PANEL);
        } else if (childHandler instanceof RiscoBusExpanderHandler) {
            RiscoBusExpanderHandler handler = (RiscoBusExpanderHandler) childHandler;
            thingBusExpanderMap.remove(handler.getBusExpanderNumber());
        } else if (childHandler instanceof RiscoKeyfobHandler) {
            RiscoKeyfobHandler handler = (RiscoKeyfobHandler) childHandler;
            thingKeyfobMap.remove(handler.getKeyfobNumber());
        } else if (childHandler instanceof RiscoKeypadHandler) {
            RiscoKeypadHandler handler = (RiscoKeypadHandler) childHandler;
            thingKeypadMap.remove(handler.getKeypadNumber());
        } else if (childHandler instanceof RiscoOutputHandler) {
            RiscoOutputHandler handler = (RiscoOutputHandler) childHandler;
            thingOutputMap.remove(handler.getOutputNumber());
        } else if (childHandler instanceof RiscoOutputExpanderHandler) {
            RiscoOutputExpanderHandler handler = (RiscoOutputExpanderHandler) childHandler;
            thingOutputExpanderMap.remove(handler.getOutputExpanderNumber());
        } else if (childHandler instanceof RiscoPartitionHandler) {
            RiscoPartitionHandler handler = (RiscoPartitionHandler) childHandler;
            thingPartitionMap.remove(handler.getPartitionNumber());
        } else if (childHandler instanceof RiscoPowerSupplyHandler) {
            RiscoPowerSupplyHandler handler = (RiscoPowerSupplyHandler) childHandler;
            thingPowerSupplyMap.remove(handler.getPowerSupplyNumber());
        } else if (childHandler instanceof RiscoProximityReaderHandler) {
            RiscoProximityReaderHandler handler = (RiscoProximityReaderHandler) childHandler;
            thingPartitionMap.remove(handler.getProximityReaderNumber());
        } else if (childHandler instanceof RiscoSounderHandler) {
            RiscoSounderHandler handler = (RiscoSounderHandler) childHandler;
            thingSounderMap.remove(handler.getSounderNumber());
        } else if (childHandler instanceof RiscoVoiceModuleHandler) {
            thingGeneralMap.remove(RiscoBindingConstants.VOICE_MODULE);
        } else if (childHandler instanceof RiscoWirelessModuleHandler) {
            RiscoWirelessModuleHandler handler = (RiscoWirelessModuleHandler) childHandler;
            thingWirelessModuleMap.remove(handler.getWirelessModuleNumber());
        } else if (childHandler instanceof RiscoZoneHandler) {
            RiscoZoneHandler handler = (RiscoZoneHandler) childHandler;
            thingZoneMap.remove(handler.getZoneNumber());
        } else if (childHandler instanceof RiscoZoneExpanderHandler) {
            RiscoZoneExpanderHandler handler = (RiscoZoneExpanderHandler) childHandler;
            thingZoneExpanderMap.remove(handler.getZoneExpanderNumber());
        }

        super.childHandlerDisposed(childHandler, childThing);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(RiscoDiscoveryService.class);
    }

    /**
     * Register the Discovery Service.
     *
     * @param discoveryService
     */
    public void registerDiscoveryService(RiscoDiscoveryService discoveryService) {
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

    public void addListener(RiscoPanelListener listener) {
        RiscoCommunicator communicator = this.communicator;
        if (communicator != null) {
            communicator.addListener(listener);
        }
    }
}
