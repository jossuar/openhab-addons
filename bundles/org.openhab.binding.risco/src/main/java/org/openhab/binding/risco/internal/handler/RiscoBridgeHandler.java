/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
import org.openhab.binding.risco.internal.MessageOrigin;
import org.openhab.binding.risco.internal.RiscoCommunicator;
import org.openhab.binding.risco.internal.RiscoCommunicator.RiscoPanelListener;
import org.openhab.binding.risco.internal.RiscoDiscoveryService;
import org.openhab.binding.risco.internal.RiscoMessage;
import org.openhab.binding.risco.internal.RiscoMessagePair;
import org.openhab.binding.risco.internal.config.RiscoBridgeConfiguration;
import org.openhab.binding.risco.internal.message.RiscoMessageType;
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
                    configuration.getId(), configuration.getEncoding(), scheduler);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication cannot be initialized. " + e.toString());

            return;
        }

        RiscoCommunicator communicator = this.communicator;
        if (communicator != null) {
            communicator.addListener(this);

            communicator.send("RMT=5678");
            communicator.send("LCL");
            communicator.send("ZLBL*1:8?");
            communicator.send("ZONEQTY?");
            communicator.send("PNLCNF");
            communicator.send("SYSLBL?");
            communicator.send("SSTT?");
            communicator.send("ZTYPE*1?");
            communicator.send("ZPART&*1?");
            communicator.send("ZAREA&*1?");
            communicator.send("PNLVER?");
            communicator.send("PNLSERD?");
            communicator.send("MAINBAT?");
            communicator.send("WIFIPASS?");
            communicator.send("GETSNF1?");
            communicator.send("KRSTT1?");

            communicator.send("PSSTT1?");
            communicator.send("SNSTT1?");
            communicator.send("ZESTT1?");
            communicator.send("OESTT1?");
            communicator.send("OSTT1?");

            // communicator.send("ZAREA(@INX@)");

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
    public void handleRiscoMessage(RiscoMessagePair pair) {
        RiscoMessage msg;

        if (pair.getMessageOrigin() == MessageOrigin.BINDING) {
            msg = pair.getRequest();
        } else {
            msg = pair.getResponse();
        }

        if (msg == null) {
            return;
        }

        if (msg.getCommand().startsWith("ZSTT")) {
            logger.debug("", msg);
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
                RiscoMessageType mt = RiscoMessageType.STATUS_ZONE;
                String thingId = String.format(mt.thingIdFormat, 1);
                String thingLabel = String.format(mt.thingLabelFormat, 1);
                ThingUID thingUID = new ThingUID(mt.thingType, getThing().getUID(), mt.thingType + "1");
                discoveryService.addThing(getThing(), thingUID, thingLabel, "zoneNumber", 1);
            }
        }

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
