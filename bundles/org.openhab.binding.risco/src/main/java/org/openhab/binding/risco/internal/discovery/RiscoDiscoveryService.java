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
package org.openhab.binding.risco.internal.discovery;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.protocol.message.KeypadAllocation;
import org.openhab.binding.risco.internal.protocol.message.OutputAllocation;
import org.openhab.binding.risco.internal.protocol.message.PanelConfiguration;
import org.openhab.binding.risco.internal.protocol.message.PartitionAllocation;
import org.openhab.binding.risco.internal.protocol.message.ZoneAllocation;
import org.openhab.binding.risco.internal.protocol.message.ZoneExpanderAllocation;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for discovering the supported Things.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoDiscoveryService extends AbstractDiscoveryService implements DiscoveryService, ThingHandlerService {
    private final Logger logger = LoggerFactory.getLogger(RiscoDiscoveryService.class);

    private @Nullable RiscoBridgeHandler bridgeHandler = null;

    public RiscoDiscoveryService() {
        super(RiscoBindingConstants.SUPPORTED_THING_TYPES_UIDS, 15, false);
    }

    @Override
    protected void startScan() {
        logger.debug("startScan called");

        RiscoBridgeHandler bridge = bridgeHandler;
        if (bridge != null) {
            // Panel
            bridge.sendCommand(PanelConfiguration.COMMAND);
            // Zone Expanders
            bridge.sendCommand(ZoneExpanderAllocation.COMMAND);
            // Partitions
            bridge.sendCommand(PartitionAllocation.COMMAND);
            // Output Expanders
            bridge.sendCommand("UOALOC&");
            // Outputs
            bridge.sendCommand(OutputAllocation.COMMAND);
            // Keypads
            bridge.sendCommand(KeypadAllocation.COMMAND1);
            bridge.sendCommand(KeypadAllocation.COMMAND2);
            // Keyfobs
            bridge.sendCommand("KFALOC&");
            // Power Supplies
            bridge.sendCommand("PSALOC&");
            // Proximity readers
            bridge.sendCommand("KRALOC&");
            // Receivers
            bridge.sendCommand("WMEALOC&");
            // Sounders
            bridge.sendCommand("ODSALOC&");
            bridge.sendCommand("WSALOC&");
            // Zones
            bridge.sendCommand(ZoneAllocation.COMMAND1);
            bridge.sendCommand(ZoneAllocation.COMMAND2);
            bridge.sendCommand(ZoneAllocation.COMMAND3);
        }
    }

    public void addThing(Bridge bridge, ThingUID thingUID, String thingLabel, @Nullable String indexProperty,
            @Nullable Integer index) {
        String representationProperty = null;
        Map<String, Object> properties = null;

        if (indexProperty != null && index != null) {
            properties = Collections.singletonMap(indexProperty, index);
            representationProperty = indexProperty;
        }

        DiscoveryResult discoveryResult;
        if (properties != null && representationProperty != null) {
            discoveryResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                    .withRepresentationProperty(representationProperty).withBridge(bridge.getUID())
                    .withLabel(thingLabel).build();
        } else {
            discoveryResult = DiscoveryResultBuilder.create(thingUID).withBridge(bridge.getUID()).withLabel(thingLabel)
                    .build();
        }

        thingDiscovered(discoveryResult);

        logger.debug("Discovered {}", thingUID);
    }

    @Override
    public void activate() {
        RiscoBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            handler.registerDiscoveryService(this);
        }
    }

    @Override
    public void deactivate() {
        RiscoBridgeHandler handler = bridgeHandler;
        if (handler != null) {
            handler.unregisterDiscoveryService();
        }
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof RiscoBridgeHandler) {
            bridgeHandler = (RiscoBridgeHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return bridgeHandler;
    }
}
