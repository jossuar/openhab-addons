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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoCommunicator;
import org.openhab.binding.risco.internal.RiscoCommunicator.RiscoPanelListener;
import org.openhab.binding.risco.internal.config.RiscoBridgeConfiguration;
import org.openhab.binding.risco.internal.discovery.RiscoDiscoveryService;
import org.openhab.binding.risco.internal.handler.thing.RiscoZoneHandler;
import org.openhab.binding.risco.internal.protocol.MessageProperty;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
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

    private Map<Integer, Thing> thingZoneMap = new ConcurrentHashMap<>();

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

        if ("ZSTT".equals(msg.getCommandName())) {
            logger.debug("{}", msg);
            // parse Zone Status message
        }

        Thing thing = null;// findThing(caddxThingType, partition, zone, keypad);
        RiscoDiscoveryService discoveryService = this.discoveryService;
        if (thing != null) {
            // RiscoThingHandler thingHandler = (RiscoThingHandler) thing.getHandler();
            // if (thingHandler != null) {
            // thingHandler.caddxEventReceived(event, thing);
            // }
        } else {
            if (discoveryService != null) {
                msg.getProperties().stream().filter(distinctByKey(MessageProperty::getThingUID)).map(mp -> {
                    Integer idx;
                    String intValue = mp.getThingUID().replaceAll("[^0-9]", "");
                    idx = ("".equals(intValue)) ? null : Integer.parseInt(intValue);
                    discoveryService.addThing(getThing(),
                            new ThingUID(mp.getThingTypeUID(), getThing().getUID(), mp.getThingUID()), mp.getThingUID(),
                            "zoneNumber", idx);
                    return 1;
                });

                for (MessageProperty mp : msg.getProperties()) {
                    ThingUID thingUID = new ThingUID(mp.getThingTypeUID(), getThing().getUID(), mp.getThingUID());

                    Integer idx;
                    String intValue = mp.getThingUID().replaceAll("[^0-9]", "");
                    idx = ("".equals(intValue)) ? null : Integer.parseInt(intValue);

                    discoveryService.addThing(getThing(), thingUID, mp.getThingUID(), "zoneNumber", idx);
                }

                for (MessageProperty mp : msg.getProperties()) {
                    ThingUID thingUID = new ThingUID(mp.getThingTypeUID(), getThing().getUID(), mp.getThingUID());

                    Integer idx;
                    String intValue = mp.getThingUID().replaceAll("[^0-9]", "");
                    idx = ("".equals(intValue)) ? null : Integer.parseInt(intValue);

                    discoveryService.addThing(getThing(), thingUID, mp.getThingUID(), "zoneNumber", idx);
                }
            }
        }
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
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
