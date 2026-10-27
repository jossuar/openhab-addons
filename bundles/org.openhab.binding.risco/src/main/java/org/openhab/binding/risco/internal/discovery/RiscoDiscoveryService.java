/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
import org.openhab.binding.risco.internal.protocol.message.allocation.BusExpanderAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.KeyfobAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.KeypadAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.OutputAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.OutputExpanderAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.PartitionAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.PowerSupplyAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.ProximityReaderAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.SirenAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.WirelessModuleAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.ZoneAllocation;
import org.openhab.binding.risco.internal.protocol.message.allocation.ZoneExpanderAllocation;
import org.openhab.binding.risco.internal.protocol.message.general.PanelConfiguration;
import org.openhab.core.config.discovery.AbstractThingHandlerDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for discovering the supported Things.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@Component(scope = ServiceScope.PROTOTYPE, service = RiscoDiscoveryService.class)
@NonNullByDefault
public class RiscoDiscoveryService extends AbstractThingHandlerDiscoveryService<RiscoBridgeHandler> {
    private final Logger logger = LoggerFactory.getLogger(RiscoDiscoveryService.class);

    public RiscoDiscoveryService() {
        super(RiscoBridgeHandler.class, RiscoBindingConstants.SUPPORTED_THING_TYPES_UIDS, 15);
    }

    @Override
    protected void startScan() {
        logger.debug("startScan called");

        RiscoBridgeHandler bridge = thingHandler;

        // Bus Expanders
        bridge.sendCommand(BusExpanderAllocation.COMMAND);
        // Keyfobs
        bridge.sendCommand(KeyfobAllocation.COMMAND);
        // Keypads
        bridge.sendCommand(KeypadAllocation.COMMAND1);
        bridge.sendCommand(KeypadAllocation.COMMAND2);
        // Outputs
        bridge.sendCommand(OutputAllocation.COMMAND);
        // Output Expanders
        bridge.sendCommand(OutputExpanderAllocation.COMMAND);
        // Panel
        bridge.sendCommand(PanelConfiguration.COMMAND);
        // Partitions
        bridge.sendCommand(PartitionAllocation.COMMAND);
        // Power Supplies
        bridge.sendCommand(PowerSupplyAllocation.COMMAND);
        // Proximity Readers
        bridge.sendCommand(ProximityReaderAllocation.COMMAND);
        // Sirens
        bridge.sendCommand(SirenAllocation.COMMAND);
        // Wireless modules
        bridge.sendCommand(WirelessModuleAllocation.COMMAND);
        // Zones
        bridge.sendCommand(ZoneAllocation.COMMAND1);
        bridge.sendCommand(ZoneAllocation.COMMAND2);
        bridge.sendCommand(ZoneAllocation.COMMAND3);
        // Zone Expanders
        bridge.sendCommand(ZoneExpanderAllocation.COMMAND);
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
    public void initialize() {
        RiscoBridgeHandler handler = thingHandler;
        handler.registerDiscoveryService(this);

        super.initialize();
    }

    @Override
    public void dispose() {
        super.dispose();

        RiscoBridgeHandler handler = thingHandler;
        handler.unregisterDiscoveryService();
    }
}
