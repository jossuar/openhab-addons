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
package org.openhab.binding.mysensors.factory;

import static org.openhab.binding.mysensors.MySensorsBindingConstants.*;

import java.util.Hashtable;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.discovery.MySensorsDiscoveryService;
import org.openhab.binding.mysensors.handler.MySensorsBridgeHandler;
import org.openhab.binding.mysensors.handler.MySensorsThingHandler;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MySensorsHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Tim Oberföll - Initial contribution
 */
@NonNullByDefault
@Component(service = { ThingHandlerFactory.class })
public class MySensorsHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final SerialPortManager serialPortManager;

    @Activate
    public MySensorsHandlerFactory(@Reference SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_DEVICE_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    @Nullable
    protected ThingHandler createHandler(@Nullable Thing thing) {
        if (thing == null) {
            throw new IllegalArgumentException("Thing is null, cannot create handler");
        }
        logger.trace("Creating handler for thing: {}", thing.getUID());
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        @Nullable
        ThingHandler handler;

        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            handler = new MySensorsThingHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_BRIDGE_SER) || thingTypeUID.equals(THING_TYPE_BRIDGE_ETH)
                || thingTypeUID.equals(THING_TYPE_BRIDGE_MQTT)) {
            MySensorsBridgeHandler bridgeHandler = new MySensorsBridgeHandler((Bridge) thing, this.serialPortManager);
            registerDiscoveryService(bridgeHandler);
            handler = bridgeHandler;
        } else {
            logger.error("Thing {} cannot be configured, is this thing supported by the binding?", thingTypeUID);
            throw new IllegalArgumentException("Thing type is not supported");
        }

        return handler;
    }

    private synchronized void registerDiscoveryService(MySensorsBridgeHandler bridgeHandler) {
        MySensorsDiscoveryService discoveryService = new MySensorsDiscoveryService(bridgeHandler);
        bridgeHandler.setDiscoveryService(discoveryService);
        bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, new Hashtable<>());
    }
}
