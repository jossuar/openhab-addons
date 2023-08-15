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
package org.openhab.binding.risco.internal.handler.thing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.config.RiscoPowerSupplyConfiguration;
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.handler.RiscoThingHandler;
import org.openhab.binding.risco.internal.protocol.message.PowerSupplyStatus;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoPowerSupplyHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoPowerSupplyHandler extends RiscoThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RiscoPowerSupplyHandler.class);

    private int powerSupplyNumber;

    public RiscoPowerSupplyHandler(Thing thing) {
        super(thing);
    }

    public int getPowerSupplyNumber() {
        return powerSupplyNumber;
    }

    public void setPowerSupplyNumber(int powerSupplyNumber) {
        this.powerSupplyNumber = powerSupplyNumber;
    }

    @Override
    public void initialize() {
        // Load configuration
        RiscoPowerSupplyConfiguration config = getConfigAs(RiscoPowerSupplyConfiguration.class);
        setPowerSupplyNumber(config.getPowerSupplyNumber());

        // set the Thing offline for now
        updateStatus(ThingStatus.OFFLINE);

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Send Zone Status update command
        bridgeHandler.sendCommand(PowerSupplyStatus.getReadCommand(powerSupplyNumber));
        logger.trace("RiscoPartitionHandler initialized [{}]", powerSupplyNumber);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        List<String> messages = new ArrayList<String>();

        if (command instanceof RefreshType) {
            messages.add(PowerSupplyStatus.getReadCommand(getPowerSupplyNumber()));
        } else {
            logger.debug("Unknown command {}", command);
            return;
        }

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        for (String m : messages) {
            bridgeHandler.sendCommand(m);
        }
    }
}
