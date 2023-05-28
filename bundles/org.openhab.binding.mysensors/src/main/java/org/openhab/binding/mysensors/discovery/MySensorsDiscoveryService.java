/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.mysensors.discovery;

import static org.openhab.binding.mysensors.MySensorsBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.handler.MySensorsBridgeHandler;
import org.openhab.binding.mysensors.internal.event.MySensorsGatewayEventListener;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGateway;
import org.openhab.binding.mysensors.internal.sensors.MySensorsChild;
import org.openhab.binding.mysensors.internal.sensors.MySensorsNode;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service for MySensors devices. Starts DiscoveryThread to listen for
 * new things / nodes.
 *
 * @author Tim Oberföll - Initial contribution
 *
 */
@NonNullByDefault
public class MySensorsDiscoveryService extends AbstractDiscoveryService implements MySensorsGatewayEventListener {

    private final Logger logger = LoggerFactory.getLogger(MySensorsDiscoveryService.class);

    private final MySensorsBridgeHandler bridgeHandler;

    public MySensorsDiscoveryService(MySensorsBridgeHandler bridgeHandler) {
        super(SUPPORTED_THING_TYPES_UIDS, 500, true);
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    protected void startScan() {
        logger.debug("Starting MySensors discovery scan");
        @Nullable
        MySensorsGateway gw = bridgeHandler.getMySensorsGateway();
        if (gw != null) {
            gw.addEventListener(this);
        } else {
            logger.warn("Can't start scanning for discovery with null gateway");
        }
    }

    public void activate() {
        startScan();
    }

    @Override
    public void deactivate() {
        stopScan();
    }

    @Override
    protected void stopScan() {
        logger.debug("Stopping MySensors discovery scan");
        @Nullable
        MySensorsGateway gw = bridgeHandler.getMySensorsGateway();

        if (gw != null) {
            gw.removeEventListener(this);
        }
    }

    @Override
    protected void startBackgroundDiscovery() {
        startScan();
    }

    @Override
    protected void stopBackgroundDiscovery() {
        stopScan();
    }

    /**
     * Gets called if message from the MySensors network was received.
     * Distinguishes if a new thing was discovered.
     *
     *
     */
    public void newDevicePresented(@Nullable MySensorsNode node, @Nullable MySensorsChild child) {
        /*
         * If a message was received from a not known node, which is not a
         * presentation message, we don't do anything!
         */
        if (child != null && node != null) {
            // uid must not contains dots
            ThingTypeUID thingUid = THING_UID_MAP.get(child.getPresentationCode());

            if (thingUid != null) {
                logger.debug("Preparing new thing for inbox: {}", thingUid);

                ThingUID uid = new ThingUID(thingUid, bridgeHandler.getThing().getUID(),
                        thingUid.getId().toLowerCase() + "_" + node.getNodeId() + "_" + child.getChildId());

                Map<String, Object> properties = new HashMap<>(2);
                properties.put(PARAMETER_NODEID, node.getNodeId());
                properties.put(PARAMETER_CHILDID, child.getChildId());
                DiscoveryResult result = DiscoveryResultBuilder.create(uid).withProperties(properties)
                        .withLabel("MySensors Device (" + node.getNodeId() + ";" + child.getChildId() + ")")
                        .withBridge(bridgeHandler.getThing().getUID()).build();
                thingDiscovered(result);

                logger.debug("Discovered device submitted");
            } else {
                logger.warn("Cannot automatic discover thing node: {}, child: {} please insert it manually",
                        node.getNodeId(), child.getChildId());
            }
        } else {
            logger.warn("Cannot present device with null node or child");
        }
    }

    @Override
    public void newNodeDiscovered(@Nullable MySensorsNode node, @Nullable MySensorsChild child) {
        if (node != null && child != null) {
            newDevicePresented(node, child);
        } else {
            logger.warn("Cannot discover null node and/or child");
        }
    }
}
