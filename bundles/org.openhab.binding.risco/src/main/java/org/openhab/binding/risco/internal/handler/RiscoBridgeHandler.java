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
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.RiscoCommunicator;
import org.openhab.binding.risco.internal.RiscoCommunicator.RiscoPanelListener;
import org.openhab.binding.risco.internal.config.RiscoBridgeConfiguration;
import org.openhab.binding.risco.internal.discovery.RiscoDiscoveryService;
import org.openhab.binding.risco.internal.message.RiscoMessage;
import org.openhab.binding.risco.internal.message.RiscoMessagePair;
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
                    configuration.getId(), configuration.getEncoding(), configuration.getPassword(), scheduler);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Communication cannot be initialized. " + e.toString());

            return;
        }

        try {
            Thread.sleep(12000);
        } catch (InterruptedException e) {
        }

        RiscoCommunicator communicator = this.communicator;
        if (communicator != null) {
            communicator.addListener(this);
            communicator.send("PNLCNF?");
            communicator.send("ZONEQTY?");
            communicator.send("ZLBL1?");
            communicator.send("ZLBL*1:8?");
            communicator.send("DTYPZ*1:8?");
            communicator.send("ZSTT19?");
            communicator.send("ZSTT*1:8?");

            communicator.send("SSTT?");
            communicator.send("ZPART&*1?");
            communicator.send("ZPART1?");
            communicator.send("ZPART*1:8?");
            communicator.send("PNLVER?");
            communicator.send("PNLSERD?");

            communicator.send("ZLBL*1:9?");

            // communicator.send("ZSTT1?");
            // communicator.send("ZSTT2?");
            // communicator.send("ZSTT3?");
            // communicator.send("ZSTT4?");
            // communicator.send("ZSTT5?");
            // communicator.send("ZSTT6?");
            // communicator.send("ZSTT7?");
            // communicator.send("ZSTT8?");
            // communicator.send("ZSTT9?");
            // communicator.send("ZSTT10?");
            // communicator.send("ZSTT11?");
            // communicator.send("ZSTT12?");
            // communicator.send("ZSTT13?");
            // communicator.send("ZSTT14?");
            // communicator.send("ZSTT15?");
            // communicator.send("ZSTT16?");
            // communicator.send("ZSTT17?");
            // communicator.send("ZSTT18?");
            // communicator.send("ZSTT19?");
            // communicator.send("ZSTT20?");
            // communicator.send("ZSTT21?");
            // communicator.send("ZSTT22?");
            // communicator.send("ZSTT23?");
            // communicator.send("ZSTT24?");
            // communicator.send("ZSTT25?");
            // communicator.send("ZSTT26?");
            // communicator.send("ZSTT27?");
            // communicator.send("ZSTT28?");
            // communicator.send("ZSTT29?");
            // communicator.send("ZSTT30?");
            //
            // communicator.send("DTYPZ1?");
            // communicator.send("DTYPZ2?");
            // communicator.send("DTYPZ3?");
            // communicator.send("DTYPZ4?");
            // communicator.send("DTYPZ5?");
            // communicator.send("DTYPZ6?");
            // communicator.send("DTYPZ7?");
            // communicator.send("DTYPZ8?");
            // communicator.send("DTYPZ9?");
            // communicator.send("DTYPZ10?");
            // communicator.send("DTYPZ11?");
            // communicator.send("DTYPZ12?");
            // communicator.send("DTYPZ13?");
            // communicator.send("DTYPZ14?");
            // communicator.send("DTYPZ15?");
            // communicator.send("DTYPZ16?");
            // communicator.send("DTYPZ17?");
            // communicator.send("DTYPZ18?");
            // communicator.send("DTYPZ19?");
            // communicator.send("DTYPZ20?");
            // communicator.send("DTYPZ21?");
            // communicator.send("DTYPZ22?");
            // communicator.send("DTYPZ23?");
            // communicator.send("DTYPZ24?");
            // communicator.send("DTYPZ25?");
            // communicator.send("DTYPZ26?");
            // communicator.send("DTYPZ27?");
            // communicator.send("DTYPZ28?");
            // communicator.send("DTYPZ29?");
            // communicator.send("DTYPZ30?");
            // communicator.send("ZLBL*1:8?");
            // communicator.send("ZONEQTY?");
            // communicator.send("PNLCNF");
            // communicator.send("SYSLBL?");
            // communicator.send("SSTT?");
            // communicator.send("ZTYPE*1?");
            // communicator.send("ZPART&*1?");
            // communicator.send("ZAREA&*1?");
            // communicator.send("PNLVER?");
            // communicator.send("PNLSERD?");
            // communicator.send("MAINBAT?");
            // communicator.send("WIFIPASS?");
            // communicator.send("GETSNF1?");
            // communicator.send("KRSTT1?");
            //
            // communicator.send("PSSTT1?");
            // communicator.send("SNSTT1?");
            // communicator.send("ZESTT1?");
            // communicator.send("OESTT1?");
            // communicator.send("OSTT1?");

            // communicator.send("ZAREA(@INX@)");

            updateStatus(ThingStatus.ONLINE);
        }

        // list all channels
        if (logger.isTraceEnabled())

        {
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

        msg = pair.getResponse();
        // if (pair.getMessageOrigin() == MessageOrigin.BINDING) {
        // msg = pair.getRequest();
        // } else {
        // msg = pair.getResponse();
        // }

        if (msg == null) {
            return;
        }

        if ("ZSTT".equals(msg.getCommandName())) {
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
            if (discoveryService != null && "ZSTT".equals(msg.getCommandName())) {
                RiscoMessageType mt = RiscoMessageType.STATUS_ZONE;
                int index = msg.getIndexFrom();
                String thingId = String.format(mt.thingIdFormat, index);
                String thingLabel = String.format(mt.thingLabelFormat, index);
                ThingUID thingUID = new ThingUID(RiscoBindingConstants.ZONE_THING_TYPE, getThing().getUID(), thingId);
                discoveryService.addThing(getThing(), thingUID, thingLabel, "zoneNumber", index);
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
