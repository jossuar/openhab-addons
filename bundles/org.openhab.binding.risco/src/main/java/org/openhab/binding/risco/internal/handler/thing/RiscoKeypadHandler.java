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
import org.openhab.binding.risco.internal.config.RiscoKeypadConfiguration;
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.handler.RiscoThingHandler;
import org.openhab.binding.risco.internal.protocol.message.status.KeypadStatus;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoKeypadHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoKeypadHandler extends RiscoThingHandler {
    private final Logger logger = LoggerFactory.getLogger(RiscoKeypadHandler.class);

    private int keypadNumber;
    private long lastRefreshTime = 0;

    public RiscoKeypadHandler(Thing thing) {
        super(thing);
    }

    public int getKeypadNumber() {
        return keypadNumber;
    }

    public void setKeypadNumber(int keypadNumber) {
        this.keypadNumber = keypadNumber;
    }

    @Override
    public void initialize() {
        // Load configuration
        RiscoKeypadConfiguration config = getConfigAs(RiscoKeypadConfiguration.class);
        setKeypadNumber(config.getKeypadNumber());

        // set the Thing offline for now
        updateStatus(ThingStatus.OFFLINE);

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Send Siren Status update command
        bridgeHandler.sendCommand(KeypadStatus.getReadCommand(keypadNumber));
        logger.trace("RiscoKeypadHandler initialized [{}]", keypadNumber);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        List<String> messages = new ArrayList<String>();

        if (command instanceof RefreshType) {
            // Refresh only if 5 seconds have passed from the last refresh
            if (System.currentTimeMillis() - lastRefreshTime > 5000) {
                messages.add(KeypadStatus.getReadCommand(getKeypadNumber()));
                lastRefreshTime = System.currentTimeMillis();
            }
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
