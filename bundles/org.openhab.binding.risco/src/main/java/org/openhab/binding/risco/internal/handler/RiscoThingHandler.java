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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public abstract class RiscoThingHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RiscoThingHandler.class);

    private @Nullable RiscoBridgeHandler bridgeHandler;

    public RiscoThingHandler(Thing thing) {
        super(thing);
    }

    public @Nullable RiscoBridgeHandler getBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();

            if (bridge == null) {
                logger.debug("getCaddxBridgeHandler(): Unable to get bridge!");
                return null;
            }

            logger.trace("getCaddxBridgeHandler(): Bridge for '{}' - '{}'", getThing().getUID(), bridge.getUID());

            ThingHandler handler = bridge.getHandler();

            if (handler instanceof RiscoBridgeHandler) {
                this.bridgeHandler = (RiscoBridgeHandler) handler;
            } else {
                logger.debug("getBridgeHandler(): Unable to get bridge handler!");
            }
        }

        return this.bridgeHandler;
    }

}
