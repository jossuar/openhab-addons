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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.risco", service = ThingHandlerFactory.class)
public class RiscoHandlerFactory extends BaseThingHandlerFactory {

    private final Logger logger = LoggerFactory.getLogger(RiscoHandlerFactory.class);
    // private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_SAMPLE);

    @Activate
    public RiscoHandlerFactory() {
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return RiscoBindingConstants.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (RiscoBindingConstants.BRIDGE_THING_TYPE.equals(thingTypeUID)) {
            return new RiscoBridgeHandler((Bridge) thing);
        } else if (RiscoBindingConstants.SYSTEM_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.PARTITION_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.ZONE_THING_TYPE.equals(thingTypeUID)) {
            return new RiscoZoneHandler(thing);
        } else if (RiscoBindingConstants.OUTPUT_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.UTILITY_OUTPUT_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.KEYPAD_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.KEYFOB_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.BUS_EXPANDER_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.ZONE_EXPANDER_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.OUTPUT_EXPANDER_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.WIRELESS_MODULE_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.VOICE_MODULE_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.CELLULAR_ON_BUS_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else if (RiscoBindingConstants.SIREN_THING_TYPE.equals(thingTypeUID)) {
            logger.debug("createHandler(): ThingHandler not implemented for {}", thingTypeUID);
        } else {
            logger.debug("createHandler(): ThingHandler not found for {}", thingTypeUID);
            return null;
        }
        return null;
    }
}
