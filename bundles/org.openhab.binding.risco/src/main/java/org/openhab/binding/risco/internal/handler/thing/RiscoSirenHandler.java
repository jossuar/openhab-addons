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
import org.openhab.binding.risco.internal.config.RiscoSirenConfiguration;
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.handler.RiscoThingHandler;
import org.openhab.binding.risco.internal.protocol.message.SirenStatus;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoSirenHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoSirenHandler extends RiscoThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RiscoSirenHandler.class);

    private int sirenNumber;

    public RiscoSirenHandler(Thing thing) {
        super(thing);
    }

    public int getSirenNumber() {
        return sirenNumber;
    }

    public void setSirenNumber(int sirenNumber) {
        this.sirenNumber = sirenNumber;
    }

    @Override
    public void initialize() {
        // Load configuration
        RiscoSirenConfiguration config = getConfigAs(RiscoSirenConfiguration.class);
        setSirenNumber(config.getSirenNumber());

        // set the Thing offline for now
        updateStatus(ThingStatus.OFFLINE);

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Send Siren Status update command
        bridgeHandler.sendCommand(SirenStatus.getReadCommand(sirenNumber));
        logger.trace("RiscoSirenHandler initialized [{}]", sirenNumber);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        List<String> messages = new ArrayList<String>();

        if (command instanceof RefreshType) {
            messages.add(SirenStatus.getReadCommand(getSirenNumber()));
        } else {
            logger.debug("Unknown command {}", command);
            return;
        }

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Send Zone Status update command
        for (String m : messages) {
            bridgeHandler.sendCommand(m);
        }
    }
}
