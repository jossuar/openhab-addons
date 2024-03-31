/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.RiscoCommunicator.RiscoPanelListener;
import org.openhab.binding.risco.internal.action.RiscoPartitionActions;
import org.openhab.binding.risco.internal.config.RiscoPartitionConfiguration;
import org.openhab.binding.risco.internal.handler.RiscoBridgeHandler;
import org.openhab.binding.risco.internal.handler.RiscoThingHandler;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionArm;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionDisarm;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionLabel;
import org.openhab.binding.risco.internal.protocol.message.general.PartitionStay;
import org.openhab.binding.risco.internal.protocol.message.general.UserPin;
import org.openhab.binding.risco.internal.protocol.message.status.PartitionStatus;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.ThingHandlerService;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoPartitionHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoPartitionHandler extends RiscoThingHandler implements RiscoPanelListener {
    private final Logger logger = LoggerFactory.getLogger(RiscoPartitionHandler.class);

    private int partitionNumber;
    private int userIndex;
    private long lastRefreshTime = 0;
    private @Nullable String userPin;

    public RiscoPartitionHandler(Thing thing) {
        super(thing);
    }

    public int getPartitionNumber() {
        return partitionNumber;
    }

    public void setPartitionNumber(int partitionNumber) {
        this.partitionNumber = partitionNumber;
    }

    public int getUserIndex() {
        return userIndex;
    }

    public void setUserIndex(int userIndex) {
        this.userIndex = userIndex;
    }

    @Override
    public void initialize() {
        // Load configuration
        RiscoPartitionConfiguration config = getConfigAs(RiscoPartitionConfiguration.class);
        setPartitionNumber(config.getPartitionNumber());
        setUserIndex(config.getUserIndex());

        // set the Thing offline for now
        updateStatus(ThingStatus.OFFLINE);

        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        // Send Partition Status update command
        bridgeHandler.sendCommand(PartitionStatus.getReadCommand(partitionNumber));
        bridgeHandler.sendCommand(PartitionLabel.getReadCommand(getPartitionNumber()));

        bridgeHandler.sendCommand(UserPin.getReadCommand(getUserIndex()));

        bridgeHandler.addListener(this);

        logger.trace("RiscoPartitionHandler initialized [{}]", partitionNumber);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("handleCommand(): Command Received - {} {}.", channelUID, command);

        List<String> messages = new ArrayList<String>();

        if (command instanceof RefreshType) {
            // Refresh only if 5 seconds have passed from the last refresh
            if (System.currentTimeMillis() - lastRefreshTime > 5000) {
                messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
                messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));
                lastRefreshTime = System.currentTimeMillis();
            }
        } else {
            logger.debug("Unknown command channel:{} command:{}", channelUID, command);
            return;
        }

        sendMessages(messages);
    }

    public void arm(String userPin) {
        if (userPin.equals(this.userPin)) {
            List<String> messages = new ArrayList<String>();

            messages.add(PartitionArm.getCommand(getPartitionNumber()));
            messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
            messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));

            sendMessages(messages);
        }
    }

    public void stay(String userPin) {
        if (userPin.equals(this.userPin)) {
            List<String> messages = new ArrayList<String>();

            messages.add(PartitionStay.getCommand(getPartitionNumber()));
            messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
            messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));

            sendMessages(messages);
        }
    }

    public void disarm(String userPin) {
        if (userPin.equals(this.userPin)) {
            List<String> messages = new ArrayList<String>();

            messages.add(PartitionDisarm.getCommand(getPartitionNumber()));
            messages.add(PartitionStatus.getReadCommand(getPartitionNumber()));
            messages.add(PartitionLabel.getReadCommand(getPartitionNumber()));

            sendMessages(messages);
        }
    }

    private void sendMessages(List<String> messages) {
        RiscoBridgeHandler bridgeHandler = getBridgeHandler();
        if (bridgeHandler == null) {
            return;
        }

        for (String m : messages) {
            bridgeHandler.sendCommand(m);
        }
    }

    @Override
    public void handleRiscoMessage(RiscoMessage msg) {
        if (UserPin.COMMAND.equals(msg.getCommandName()) && msg.getIndexFrom() == this.userIndex) {
            String[] values = msg.getCommandValues();
            if (values.length > 0) {
                this.userPin = values[0];
            }
        }
    }

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Set.of(RiscoPartitionActions.class);
    }
}
