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
package org.openhab.binding.mysensors.handler;

import static org.openhab.binding.mysensors.MySensorsBindingConstants.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.MySensorsBindingConstants;
import org.openhab.binding.mysensors.config.MySensorsSensorConfiguration;
import org.openhab.binding.mysensors.converter.MySensorsRGBWPureTypeConverter;
import org.openhab.binding.mysensors.converter.MySensorsTypeConverter;
import org.openhab.binding.mysensors.internal.event.MySensorsGatewayEventListener;
import org.openhab.binding.mysensors.internal.event.MySensorsNodeUpdateEventType;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGateway;
import org.openhab.binding.mysensors.internal.protocol.MySensorsAbstractConnection;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessage;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessageAck;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessageSubType;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessageType;
import org.openhab.binding.mysensors.internal.sensors.*;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MySensorsThingHandler} is responsible for handling commands, which are
 * sent to one of the channels and messages received via the MySensors network.
 *
 * @author Tim Oberföll - Initial contribution
 */
@NonNullByDefault
public class MySensorsThingHandler extends BaseThingHandler implements MySensorsGatewayEventListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final MySensorsSensorConfiguration configuration = getConfigAs(MySensorsSensorConfiguration.class);

    private @Nullable DateTimeType lastUpdate;

    private @Nullable MySensorsGateway myGateway;

    public MySensorsThingHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.trace("Configuration: {}", configuration.toString());
        @Nullable
        MySensorsBridgeHandler bridgeHandler = getBridgeHandler();
        myGateway = (bridgeHandler != null) ? bridgeHandler.getMySensorsGateway() : null;

        if (myGateway != null) {
            addIntoGateway(getThing(), configuration);
            registerListeners();
            updateStatus(ThingStatus.ONLINE);
            logger.debug("Initialized thing {}", getThing().getUID());
        } else {
            logger.warn("Attempted to initialize a Thing Handler with a null gateway: {}", configuration.toString());
        }
    }

    @Override
    public void dispose() {
        updateStatus(ThingStatus.OFFLINE);
        if (myGateway != null) {
            myGateway.removeEventListener(this);

            // Removing node when thing goes offline removes other children
            // myGateway.removeNode(configuration.nodeId);
        } else {
            logger.warn("Attempted to dispose a null MySensors gateway");
        }
        logger.debug("Disposed of thing {}", getThing().getUID());

        super.dispose();
    }

    @Override
    public void thingUpdated(Thing thing) {
        logger.debug("ThingHandler thing updated {}", thing.getUID());
        super.thingUpdated(thing);
    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {
        logger.debug("MySensors Bridge Status updated to {} for device: {}", bridgeStatusInfo.getStatus(),
                getThing().getUID().toString());
        if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE || bridgeStatusInfo.getStatus() == ThingStatus.OFFLINE) {
            if (bridgeStatusInfo.getStatus() == ThingStatus.ONLINE) {
                registerListeners();
            } else if (myGateway != null) {
                myGateway.removeEventListener(this);
            } else {
                logger.warn("Attempted to remove gateway listener on null gateway {}", configuration.toString());
            }
        }
        updateStatus(bridgeStatusInfo.getStatus());
        super.bridgeStatusChanged(bridgeStatusInfo);
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        logger.debug("Handle configuration update on {}", thing.getUID());
        logger.trace("Configuration update for thing {}-{}: {}", configuration.nodeId, configuration.childId,
                configurationParameters);
        super.handleConfigurationUpdate(configurationParameters);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (myGateway == null) {
            logger.warn("Attempted to handle command for null gateway: {}", configuration.toString());
            return;
        }

        logger.trace("Command {} received for channel uid {}", command, channelUID);

        // We don't handle refresh commands yet
        if (command == RefreshType.REFRESH) {
            return;
        }

        int intRequestAck = configuration.requestAck ? 1 : 0;

        // just forward the message in case it is received via this channel. This is special!
        if (channelUID.getId().equals(CHANNEL_MYSENSORS_MESSAGE)) {
            if (command instanceof StringType) {
                StringType stringTypeMessage = (StringType) command;
                try {
                    MySensorsMessage msg = MySensorsMessage.parse(stringTypeMessage.toString());

                    myGateway.sendMessage(msg);
                } catch (ParseException e) {
                    logger.error("Invalid message to send", e);
                }
            }
        } else {
            MySensorsTypeConverter adapter;

            // RGB && RGBW only:
            // if the brightness (Percentage) is changed it must be send via V_PERCENTAGE
            // and another converter must be used
            boolean rgbPercentageValue = false;
            boolean rgbOnOffValue = false;
            if ((channelUID.getId().equals(CHANNEL_RGB) || channelUID.getId().equals(CHANNEL_RGBW))
                    && (command instanceof OnOffType)) {
                adapter = loadAdapterForChannelType(CHANNEL_STATUS);
                rgbOnOffValue = true;
            } else if ((channelUID.getId().equals(CHANNEL_RGB) || channelUID.getId().equals(CHANNEL_RGBW))
                    && !(command instanceof HSBType)) {
                adapter = loadAdapterForChannelType(CHANNEL_PERCENTAGE);
                rgbPercentageValue = true;

                // RGBW only
                // if the config is set to use pure white instead of mixed white use special converter
            } else if (channelUID.getId().equals(CHANNEL_RGBW) && configuration.usePureWhiteLightInRGBW) {
                adapter = new MySensorsRGBWPureTypeConverter();
            } else {
                adapter = loadAdapterForChannelType(channelUID.getId());
            }

            logger.debug("Adapter: {} loaded", adapter.getClass());

            logger.trace("Adapter {} found for type {}", adapter.getClass().getSimpleName(), channelUID.getId());

            MySensorsMessageSubType type = adapter.typeFromChannelCommand(channelUID.getId(), command);

            if (type != null) {
                logger.trace("Type for channel: {}, command: {} of thing {} is: {}", thing.getUID(), command,
                        thing.getUID(), type);

                MySensorsVariable var = myGateway.getVariable(configuration.nodeId, configuration.childId, type);

                if (var != null) {
                    MySensorsMessageSubType subType;
                    if (rgbPercentageValue) {
                        subType = MySensorsMessageSubType.V_PERCENTAGE;
                    } else if (rgbOnOffValue) {
                        subType = MySensorsMessageSubType.V_STATUS;
                    } else {
                        subType = var.getType();
                    }

                    // Create the real message to send
                    MySensorsMessage newMsg = new MySensorsMessage(configuration.nodeId, configuration.childId,
                            MySensorsMessageType.SET, MySensorsMessageAck.getById(intRequestAck),
                            configuration.revertState, configuration.smartSleep);

                    newMsg.setSubType(subType);
                    newMsg.setMsg(adapter.fromCommand(command));

                    myGateway.sendMessage(newMsg);
                } else {
                    logger.warn("Variable not found, cannot handle command for thing {} of type {}", thing.getUID(),
                            channelUID.getId());
                }
            } else {
                logger.error("Could not get type of variable for channel: {}, command: {} of thing {}", thing.getUID(),
                        command, thing.getUID());
            }
        }
    }

    @Override
    public void messageReceived(@Nullable MySensorsMessage message) {
        if (message != null) {
            handleIncomingMessageEvent(message);
        } else {
            logger.warn("Cannot handle incoming null message on thing: {}", thing.getUID());
        }
    }

    @Override
    public void sensorUpdateEvent(@Nullable MySensorsNode node, @Nullable MySensorsChild child,
            @Nullable MySensorsVariable var, @Nullable MySensorsNodeUpdateEventType eventType) {
        if (eventType == null) {
            logger.warn("Sensor Update Event is triggered but eventType is null, skipping");
            return;
        }

        switch (eventType) {
            case UPDATE:
            case REVERT:
                if (node == null) {
                    logger.warn("Tried to REVERT sensor update on null node");
                    return;
                }

                if (child == null) {
                    logger.warn("Cannot associate REVERT event to null child");
                    return;
                }

                if (var == null) {
                    logger.warn("Cannot update REVERT event with null variable");
                    return;
                }
                if ((node.getNodeId() == configuration.nodeId) && (child.getChildId() == configuration.childId)) {
                    handleChildUpdateEvent(var);
                    updateLastUpdate(node, eventType == MySensorsNodeUpdateEventType.REVERT);
                }
                break;
            case BATTERY:
                if (node == null) {
                    logger.warn("Tried to BATTERY update on null node");
                    return;
                }
                if (node.getNodeId() == configuration.nodeId) {
                    handleBatteryUpdateEvent(node);
                    updateLastUpdate(node, false);
                }
                break;
        }
    }

    @Override
    public void connectionStatusUpdate(@Nullable MySensorsAbstractConnection connection, boolean connected) {
        logger.debug("Connection status update {} statusConnected:{}", thing.getUID(), connected);
        if (!connected) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        } else {
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void nodeReachStatusChanged(@Nullable MySensorsNode node, boolean reach) {
        logger.debug("Node reach status changed {} reachable:{}", thing.getUID(), reach);

        if (node == null) {
            logger.warn("Cannot handle reach state change on null node, thing: {}", thing.getUID());
            return;
        }
        if (node.getNodeId() == configuration.nodeId) {
            if (!reach) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
            } else {
                updateStatus(ThingStatus.ONLINE);
            }
        }
    }

    /**
     * For every thing there is a lastUpdate channel in which the date/time is stored
     * a message was received from this thing.
     */
    private void updateLastUpdate(MySensorsNode node, boolean isRevert) {
        // Don't always fire last update channel, do it only after a minute by
        if (lastUpdate == null
                || (System.currentTimeMillis() > (lastUpdate.getZonedDateTime().toInstant().toEpochMilli() + 60000))
                || configuration.revertState) {
            DateTimeType dt = new DateTimeType(
                    new SimpleDateFormat(DateTimeType.DATE_PATTERN).format(node.getLastUpdate()));
            lastUpdate = dt;
            updateState(CHANNEL_LAST_UPDATE, dt);
            if (!isRevert) {
                logger.debug("Setting last update for node/child {}/{} to {}", configuration.nodeId,
                        configuration.childId, dt.toString());
            } else {
                logger.warn("Setting last update for node/child {}/{} BACK (due to revert) to {}", configuration.nodeId,
                        configuration.childId, dt.toString());
            }
        }
    }

    /**
     * Returns the BridgeHandler of the bridge/gateway to the MySensors network
     *
     * @return BridgeHandler of the bridge/gateway to the MySensors network
     */
    private synchronized @Nullable MySensorsBridgeHandler getBridgeHandler() {
        @Nullable
        Bridge bridge = getBridge();
        @Nullable
        MySensorsBridgeHandler myBridgeHandler = null;
        if (bridge != null) {
            myBridgeHandler = (MySensorsBridgeHandler) bridge.getHandler();
        }

        return myBridgeHandler;
    }

    /**
     * Update the state of a child based on a MySensorsVariable var.
     *
     * In case a V_PERCENTAGE is received for a Cover the channel CHANNEL_COVER needs to be updated
     *
     * @param var variable that needs to be updated
     */
    private void handleChildUpdateEvent(MySensorsVariable var) {
        String channelName = getChannelNameFromVar(var);
        State newState = loadAdapterForChannelType(channelName).stateFromChannel(var);
        logger.debug("Updating channel: {}({}) value to: {}", channelName, var.getType(), newState);
        if (myGateway != null && myGateway.getNode(configuration.nodeId).getChild(configuration.childId)
                .getPresentationCode() == MySensorsMessageSubType.S_COVER) {
            updateState(CHANNEL_COVER, newState);
        }
        updateState(channelName, newState);
    }

    private void handleBatteryUpdateEvent(MySensorsNode node) {
        logger.debug("Updating channel: {} value to: {}", CHANNEL_BATTERY, node.getBatteryPercent());
        updateState(CHANNEL_BATTERY, new DecimalType(node.getBatteryPercent()));
    }

    private String getChannelNameFromVar(MySensorsVariable var) {
        // Cover thing has a specific behavior
        if (getThing().getThingTypeUID() == MySensorsBindingConstants.THING_TYPE_COVER) {
            return MySensorsBindingConstants.CHANNEL_COVER;
        } else {
            return CHANNEL_MAP.getOrDefault(var.getType(), "");
        }
    }

    @Nullable
    private MySensorsTypeConverter loadAdapterForChannelType(String channelName) {
        return TYPE_MAP.get(channelName);
    }

    /**
     * If a new message is received via the MySensors bridge it is handed over to the ThingHandler
     * and processed in this method. After parsing the message the corresponding channel is updated.
     *
     * @param msg The message that was received by the MySensors gateway.
     */
    private void handleIncomingMessageEvent(MySensorsMessage msg) {
        // Am I the all knowing node that receives all messages?
        if (configuration.nodeId == MYSENSORS_NODE_ID_ALL_KNOWING
                && configuration.childId == MYSENSORS_CHILD_ID_ALL_KNOWING) {
            updateState(CHANNEL_MYSENSORS_MESSAGE,
                    new StringType(MySensorsMessage.generateAPIString(msg).replaceAll("([\\r\\n])", "")));
        }
    }

    private void registerListeners() {
        if (myGateway != null && !myGateway.isEventListenerRegistered(this)) {
            logger.debug("Event listener for node {}-{} not registered yet, registering...", configuration.nodeId,
                    configuration.childId);
            myGateway.addEventListener(this);
        }
    }

    private void addIntoGateway(Thing thing, MySensorsSensorConfiguration configuration) {
        @Nullable
        MySensorsNode node = generateNodeFromThing(thing, configuration);
        if (node == null) {
            logger.error("Attempted to add null node into gateway");
            return;
        }
        if (myGateway != null) {
            myGateway.addNode(node, true);
        } else {
            logger.error("Attempted to add thing into null gateway");
        }
    }

    private @Nullable MySensorsNode generateNodeFromThing(Thing t, MySensorsSensorConfiguration configuration) {
        @Nullable
        MySensorsNode node = null;

        try {
            Integer nodeId = t.getConfiguration().as(MySensorsSensorConfiguration.class).nodeId;
            Integer childId = t.getConfiguration().as(MySensorsSensorConfiguration.class).childId;
            MySensorsMessageSubType presentation = INVERSE_THING_UID_MAP.get(t.getThingTypeUID());

            if (presentation != null) {
                logger.debug("Build sensors from thing:{} node:{} child:{}", t.getUID(), nodeId, childId);
                logger.trace("Building sensors from thing: {}, node: {}, child: {}, presentation: {}", t.getUID(),
                        nodeId, childId, presentation);

                @Nullable
                MySensorsChild child = MySensorsChild.fromPresentation(presentation, childId);
                if (child != null) {
                    child.setChildConfig(generateChildConfig(configuration));
                    node = new MySensorsNode(nodeId);
                    node.setNodeConfig(generateNodeConfig(configuration));
                    node.addChild(child);
                } else {
                    logger.warn("Cannot generate node:{} from thing:{}, child:{} is null", nodeId, t.getUID(), childId);
                }
            } else {
                logger.error("Error on building sensors from thing: {}, node: {}, child: {}", t.getUID(), nodeId,
                        childId);
            }
        } catch (Exception e) {
            logger.error("Failing on create node/child for thing {}", thing.getUID(), e);
        }

        return node;
    }

    private MySensorsChildConfig generateChildConfig(MySensorsSensorConfiguration configuration) {
        MySensorsChildConfig ret = new MySensorsChildConfig();
        ret.setRequestAck(configuration.requestAck);
        ret.setRevertState(configuration.revertState);
        ret.setExpectUpdateTimeout(configuration.childUpdateTimeout);

        logger.trace("ChildConfig for {}/{}: {}", configuration.nodeId, configuration.childId, ret.toString());

        return ret;
    }

    private MySensorsNodeConfig generateNodeConfig(MySensorsSensorConfiguration configuration) {
        MySensorsNodeConfig ret = new MySensorsNodeConfig();
        ret.setRequestHeartbeatResponse(configuration.requestHeartbeatResponse);
        ret.setExpectUpdateTimeout(configuration.nodeUpdateTimeout);

        logger.trace("NodeConfig for {}/{}: {}", configuration.nodeId, configuration.childId, ret.toString());

        return ret;
    }
}
