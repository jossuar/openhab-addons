/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.RiscoCommunicator;
import org.openhab.binding.risco.internal.RiscoCommunicator.RiscoPanelListener;
import org.openhab.binding.risco.internal.config.RiscoBridgeConfiguration;
import org.openhab.binding.risco.internal.discovery.RiscoDiscoveryService;
import org.openhab.binding.risco.internal.handler.thing.RiscoSystemHandler;
import org.openhab.binding.risco.internal.handler.thing.RiscoZoneHandler;
import org.openhab.binding.risco.internal.protocol.DiscoveryInfo;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoThing;
import org.openhab.binding.risco.internal.protocol.RiscoThingType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
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
    private Map<Integer, Thing> thingZoneMap = new ConcurrentHashMap<>();

    // Communication
    private @Nullable RiscoCommunicator communicator = null;
    private @Nullable RiscoDiscoveryService discoveryService = null;

    public RiscoBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
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
            communicator = new RiscoCommunicator(getThing().getUID().getAsString(), hostname, port,
                    configuration.getId(), configuration.getEncoding(), configuration.getPassword(), scheduler);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication cannot be initialized. " + e.toString());

            return;
        }

        RiscoCommunicator communicator = this.communicator;
        if (communicator != null) {
            communicator.addListener(this);
            // communicator.send("PNLCNF?");
            // communicator.send("PNLVER?");
            // communicator.send("PNLSERD?");
            // communicator.send("DTYPDM?");
            // communicator.send("DTYPMAT?");
            // communicator.send("DTYPVM?");
            // communicator.send("DTYPCOB?");
            // communicator.send("DTYPGSM?");
            // communicator.send("DTYPBE1?");
            // communicator.send("DTYPZE1?");
            // communicator.send("DTYPZE2?");
            // communicator.send("DTYPZE3?");
            // communicator.send("ZSTT*25:32?");

            updateStatus(ThingStatus.ONLINE);
        }

        // list all channels
        if (logger.isTraceEnabled()) {
            logger.trace("list all {} channels:", getThing().getChannels().size());
            for (Channel c : getThing().getChannels()) {
                logger.trace("Channel Type {} UID {}", c.getChannelTypeUID(), c.getUID());
            }
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
        // Get the list of things
        Thing thing = null;

        for (RiscoThing rt : msg.getThings()) {
            thing = findThing(rt.getRiscoThingType(), rt.getIndex());

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
            case SYSTEM:
                return thingGeneralMap.get(RiscoBindingConstants.SYSTEM);
            case ZONE:
                if (index != null) {
                    return thingZoneMap.get(Integer.valueOf(index));
                }
            case BUS_EXPANDER:
                break;
            case CELLULAR_ON_BUS:
                break;
            case KEYFOB:
                break;
            case KEYPAD:
                break;
            case OUTPUT:
                break;
            case OUTPUT_EXPANDER:
                break;
            case PARTITION:
                break;
            case SIREN:
                break;
            case VOICE_MODULE:
                break;
            case WIRELESS_MODULE:
                break;
            case ZONE_EXPANDER:
                break;
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
            case SYSTEM:
                ttUID = RiscoBindingConstants.SYSTEM_THING_TYPE;
                prefix = RiscoBindingConstants.SYSTEM;
                label = "System";
                indexProperty = null;
                break;
            case PARTITION:
                ttUID = RiscoBindingConstants.PARTITION_THING_TYPE;
                prefix = RiscoBindingConstants.PARTITION;
                label = "Partition " + index;
                indexProperty = "partitionNumber";
                break;
            case ZONE:
                ttUID = RiscoBindingConstants.ZONE_THING_TYPE;
                prefix = RiscoBindingConstants.ZONE;
                label = "Zone " + index;
                indexProperty = "zoneNumber";
                break;
            case KEYPAD:
                ttUID = RiscoBindingConstants.KEYPAD_THING_TYPE;
                prefix = RiscoBindingConstants.KEYPAD;
                label = "Keypad " + index;
                indexProperty = "keypadNumber";
                break;
            case OUTPUT:
                ttUID = RiscoBindingConstants.OUTPUT_THING_TYPE;
                prefix = RiscoBindingConstants.OUTPUT;
                label = "Output " + index;
                indexProperty = "outputNumber";
                break;
            case KEYFOB:
                ttUID = RiscoBindingConstants.KEYFOB_THING_TYPE;
                prefix = RiscoBindingConstants.KEYFOB;
                label = "Keyfob " + index;
                indexProperty = "keyfobNumber";
                break;
            case BUS_EXPANDER:
                ttUID = RiscoBindingConstants.BUS_EXPANDER_THING_TYPE;
                prefix = RiscoBindingConstants.BUS_EXPANDER;
                label = "Bus Expander " + index;
                indexProperty = "busExpanderNumber";
                break;
            case ZONE_EXPANDER:
                ttUID = RiscoBindingConstants.ZONE_EXPANDER_THING_TYPE;
                prefix = RiscoBindingConstants.ZONE_EXPANDER;
                label = "Zone Expander " + index;
                indexProperty = "zoneExpanderNumber";
                break;
            case OUTPUT_EXPANDER:
                ttUID = RiscoBindingConstants.OUTPUT_EXPANDER_THING_TYPE;
                prefix = RiscoBindingConstants.OUTPUT_EXPANDER;
                label = "Output Expander " + index;
                indexProperty = "outputExpanderNumber";
                break;
            case WIRELESS_MODULE:
                ttUID = RiscoBindingConstants.WIRELESS_MODULE_THING_TYPE;
                prefix = RiscoBindingConstants.WIRELESS_MODULE;
                label = "Wireless Module " + index;
                indexProperty = "wirelesModuleNumber";
                break;
            case VOICE_MODULE:
                ttUID = RiscoBindingConstants.VOICE_MODULE_THING_TYPE;
                prefix = RiscoBindingConstants.VOICE_MODULE;
                label = "Voice Module " + index;
                indexProperty = "voiceModuleNumber";
                break;
            case CELLULAR_ON_BUS:
                ttUID = RiscoBindingConstants.CELLULAR_ON_BUS_THING_TYPE;
                prefix = RiscoBindingConstants.CELLULAR_ON_BUS;
                label = "Cellular on Bus " + index;
                indexProperty = "cellularOnBusNumber";
                break;
            case SIREN:
                ttUID = RiscoBindingConstants.SIREN_THING_TYPE;
                prefix = RiscoBindingConstants.SIREN;
                label = "Siren " + index;
                indexProperty = "sirenNumber";
                break;
            default:
                logger.debug("getThingUID: Missing enum case");
                throw new IllegalArgumentException("getThingUID: type is unknown. [" + type + "]");
        }

        ThingUID thingUID = new ThingUID(ttUID, getThing().getUID(), prefix + index);
        DiscoveryInfo discoveryInfo = new DiscoveryInfo(thingUID, label, indexProperty, index);

        return discoveryInfo;
    }

    /**
     * Sends a command to the panel
     *
     * @param command The command to be send
     * @param data The associated command data
     */
    public boolean sendCommand(String command) {
        logger.trace("sendCommand(): Attempting to send Command: command - {}", command);

        RiscoCommunicator comm = communicator;
        if (comm != null) {
            comm.send(command);
        }

        return true;
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof RiscoZoneHandler) {
            RiscoZoneHandler handler = (RiscoZoneHandler) childHandler;
            thingZoneMap.put(handler.getZoneNumber(), childThing);
        } else if (childHandler instanceof RiscoSystemHandler) {
            thingGeneralMap.put(RiscoBindingConstants.SYSTEM, childThing);
        }

        super.childHandlerInitialized(childHandler, childThing);
    }

    @Override
    public void childHandlerDisposed(ThingHandler childHandler, Thing childThing) {
        if (childHandler instanceof RiscoZoneHandler) {
            RiscoZoneHandler handler = (RiscoZoneHandler) childHandler;
            thingZoneMap.remove(handler.getZoneNumber());
        }

        super.childHandlerDisposed(childHandler, childThing);
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(RiscoDiscoveryService.class);
        // Set<Class<? extends ThingHandlerService>> set = new HashSet<Class<? extends ThingHandlerService>>(2);
        // set.add(RiscoDiscoveryService.class);
        // set.add(RiscoBridgeActions.class);
        // return set;
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
}
