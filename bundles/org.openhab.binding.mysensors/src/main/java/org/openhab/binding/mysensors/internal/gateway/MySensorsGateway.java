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
package org.openhab.binding.mysensors.internal.gateway;

import java.util.*;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.internal.event.MySensorsEventRegister;
import org.openhab.binding.mysensors.internal.event.MySensorsGatewayEventListener;
import org.openhab.binding.mysensors.internal.event.MySensorsNodeUpdateEventType;
import org.openhab.binding.mysensors.internal.exception.MergeException;
import org.openhab.binding.mysensors.internal.exception.NoMoreIdsException;
import org.openhab.binding.mysensors.internal.protocol.MySensorsAbstractConnection;
import org.openhab.binding.mysensors.internal.protocol.ip.MySensorsIpConnection;
import org.openhab.binding.mysensors.internal.protocol.message.*;
import org.openhab.binding.mysensors.internal.protocol.mqtt.MySensorsMqttConnection;
import org.openhab.binding.mysensors.internal.protocol.serial.MySensorsSerialConnection;
import org.openhab.binding.mysensors.internal.sensors.MySensorsChild;
import org.openhab.binding.mysensors.internal.sensors.MySensorsNode;
import org.openhab.binding.mysensors.internal.sensors.MySensorsVariable;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main access point of all the function of MySensors Network, some of there are
 * -ID handling for the MySensors network: Requests for IDs get answered and IDs get stored in a local cache.
 * -Updating sensors variable and status information
 *
 * @author Andrea Cioni - Initial contribution
 *
 */
@NonNullByDefault
public class MySensorsGateway implements MySensorsGatewayEventListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<Integer, MySensorsNode> nodeMap;

    private final MySensorsEventRegister myEventRegister;

    @Nullable
    private MySensorsAbstractConnection myCon;

    private MySensorsGatewayConfig myConf;

    @Nullable
    private MySensorsNetworkSanityChecker myNetSanCheck;

    private final SerialPortManager serialPortManager;

    public MySensorsGateway(Map<Integer, MySensorsNode> nodeMap, SerialPortManager serialPortManager) {
        this.nodeMap = nodeMap;
        this.myEventRegister = new MySensorsEventRegister();
        this.serialPortManager = serialPortManager;
        myConf = new MySensorsGatewayConfig();
    }

    public void setMyConf(MySensorsGatewayConfig myConf) {
        this.myConf = myConf;
    }

    /**
     * Build up the gateway following given configuration parameters. Gateway will not start after this method returns.
     * Use startup to do that
     *
     * @param myConf a valid instance of {@link MySensorsGatewayConfig}
     *
     * @return true if setup done correctly
     */
    public boolean setup(MySensorsGatewayConfig myConf) {
        if (myCon != null) {
            throw new IllegalStateException("Connection is already instantiated");
        }

        setMyConf(myConf);

        switch (myConf.getGatewayType()) {
            case SERIAL:
                myCon = new MySensorsSerialConnection(myConf, myEventRegister, serialPortManager);
                return true;
            case IP:
                myCon = new MySensorsIpConnection(myConf, myEventRegister);
                return true;
            case MQTT:
                myCon = new MySensorsMqttConnection(myConf, myEventRegister);
                return true;
        }

        return false;
    }

    /**
     * Startup the gateway
     */
    public void startup() {
        if (myCon != null) {
            myCon.initialize();
        } else {
            throw new IllegalStateException("Cannot start gateway with a null connection");
        }

        myEventRegister.addEventListener(this);

        if (myConf.getEnableNetworkSanCheck() && myCon != null) {
            myNetSanCheck = new MySensorsNetworkSanityChecker(this, myEventRegister, myCon);
        }
    }

    /**
     * Shutdown the gateway
     */
    public void shutdown() {
        if (myNetSanCheck != null) {
            myNetSanCheck.stop();
        }

        if (myCon != null) {
            myCon.destroy();
            myCon = null;
        }

        myEventRegister.clearAllListeners();
    }

    /**
     * Get node from the gatway
     *
     * @param nodeId the node to retrieve
     *
     * @return node if exist or null instead
     */
    @Nullable
    public MySensorsNode getNode(int nodeId) {
        synchronized (nodeMap) {
            return nodeMap.get(nodeId);
        }
    }

    public void removeNode(int nodeId) {
        synchronized (nodeMap) {
            nodeMap.remove(nodeId);
        }
    }

    /**
     * Get a child from a node
     *
     * @param nodeId the node of the searched child
     * @param childId the child of a node
     *
     * @return child if exist or null instead
     */
    @Nullable
    public MySensorsChild getChild(int nodeId, int childId) {
        @Nullable
        MySensorsChild child = null;
        @Nullable
        MySensorsNode node = getNode(nodeId);
        if (node != null) {
            child = node.getChild(childId);
        }

        return child;
    }

    /**
     * Get a variable from a child in a node
     *
     * @param nodeId the node of the variable
     *
     * @param childId the child of the variable
     *
     * @param type the variable type (see sub-type of SET/REQ message in API documentation)
     *
     * @return variable if exist or null instead
     */
    @Nullable
    public MySensorsVariable getVariable(int nodeId, int childId, MySensorsMessageSubType type) {
        @Nullable
        MySensorsVariable variable = null;
        @Nullable
        MySensorsChild child = getChild(nodeId, childId);
        if (child != null) {
            variable = child.getVariable(type);
        } else {
            logger.warn("Cannot get variable {} from node:{}, child:{} does not exist", type, nodeId, childId);
        }

        return variable;
    }

    /**
     * Simple method that add node to gateway (only if node is not present previously).
     * This function never fail.
     *
     * @param node the node to add
     */
    public void addNode(MySensorsNode node) {
        synchronized (nodeMap) {
            if (nodeMap.containsKey(node.getNodeId())) {
                logger.warn("Overwriting previous node, it was lost.");
            }
            nodeMap.put(node.getNodeId(), node);
        }
    }

    /**
     * Add node to gateway
     *
     * @param node the node to add
     * @param mergeIfExist if true and node is already present that two nodes will be merged in one
     *
     *
     * @throws MergeException if mergeIfExist is true and nodes has common child/children
     */
    public void addNode(MySensorsNode node, boolean mergeIfExist) throws MergeException {
        synchronized (nodeMap) {
            @Nullable
            MySensorsNode exist = getNode(node.getNodeId());

            if (mergeIfExist && exist != null) {
                logger.trace("Merging child map: {} with: {}", exist.getChildMap(), node.getChildMap());

                exist.merge(node);

                logger.trace("Merging result is: {}", exist.getChildMap());
            } else {
                logger.debug("Adding node {}", node.getNodeId());
                logger.trace("Adding device {}", node.toString());
                addNode(node);
            }
        }
    }

    /**
     * @return a Set of Ids that is already used and known to the binding.
     */
    public List<Integer> getGivenIds() {
        synchronized (nodeMap) {
            return new ArrayList<>(nodeMap.keySet());
        }
    }

    /**
     * Reserve an id for network, mainly for request id messages
     *
     * @return a free id not present in node map
     * @throws NoMoreIdsException if no more ids are available to be reserved
     */
    public Integer reserveId() throws NoMoreIdsException {
        int newId = 1;

        synchronized (nodeMap) {
            List<Integer> takenIds = getGivenIds();
            while (newId < MySensorsNode.MYSENSORS_NODE_ID_RESERVED_255) {
                if (!takenIds.contains(newId)) {
                    addNode(new MySensorsNode(newId));
                    break;
                } else {
                    newId++;
                }
            }
        }

        if (newId == MySensorsNode.MYSENSORS_NODE_ID_RESERVED_255) {
            throw new NoMoreIdsException();
        }

        myEventRegister.notifyNodeIdReserved(newId);

        return newId;
    }

    /**
     * Add a {@link MySensorsGatewayEventListener} event listener to this gateway
     *
     * @param listener is the gateway listener
     */
    public void addEventListener(MySensorsGatewayEventListener listener) {
        myEventRegister.addEventListener(listener);
    }

    /**
     *
     * Remove a {@link MySensorsGatewayEventListener} event listener from this gateway
     *
     * @param listener is the gateway listener
     */
    public void removeEventListener(MySensorsGatewayEventListener listener) {
        myEventRegister.removeEventListener(listener);
    }

    /**
     * Check if a {@link MySensorsGatewayEventListener} is already registered
     *
     * @param listener is the gateway listener
     *
     * @return true if listener is already registered
     */
    public boolean isEventListenerRegistered(MySensorsGatewayEventListener listener) {
        return myEventRegister.isEventListenerRegisterd(listener);
    }

    /**
     * Send a message through this gateway. If message is of type SET will update variable state of a node/child,
     * this will trigger the update event on the {@link MySensorsEventRegister}
     *
     * @param message to send
     */
    public void sendMessage(MySensorsMessage message) {

        try {
            handleOutgoingMessage(message);
        } catch (Exception e) {
            logger.error("Handling outgoing message throw an exception", e);
        }
        if (myCon != null) {
            logger.debug("MySensorsGateway sending message {}", getConfiguration().getGatewayType());
            myCon.sendMessage(message);
        }
    }

    @Override
    public void messageReceived(@Nullable MySensorsMessage message) {
        if (message != null && !handleIncomingMessage(message)) {
            handleSpecialMessageEvent(message);
        }
    }

    @Override
    public void ackNotReceived(@Nullable MySensorsMessage msg) throws Exception {
        if (msg == null) {
            throw new IllegalArgumentException("Cannot handle Ack Not Received with null message");
        }
        if (MySensorsNode.isValidNodeId(msg.getNodeId()) && MySensorsChild.isValidChildId(msg.getChildId())
                && msg.isSetReqMessage()) {
            @Nullable
            MySensorsNode node = getNode(msg.getNodeId());
            if (node != null) {
                logger.debug("Node {} found in gateway", msg.getNodeId());

                @Nullable
                MySensorsChild child = node.getChild(msg.getChildId());
                if (child != null) {
                    logger.debug("Child {} found in node {}", msg.getChildId(), msg.getNodeId());

                    @Nullable
                    MySensorsVariable variable = child.getVariable(msg.getSubType());
                    if (variable != null) {
                        if (variable.isRevertible()) {
                            logger.debug("Variable {} found, it will be reverted to last know state",
                                    variable.getClass().getSimpleName());
                            variable.revertValue();
                            myEventRegister.notifyNodeUpdateEvent(node, child, variable,
                                    MySensorsNodeUpdateEventType.REVERT);
                        } else {
                            logger.error("Could not revert variable {}, no previous value is present",
                                    variable.getClass().getSimpleName());
                        }
                    } else {
                        logger.warn("Variable {} not present", msg.getSubType());
                    }
                } else {
                    logger.warn("Cannot handle no-ack for null child on node {}", msg.getNodeId());
                }
            }
        }
    }

    @Override
    public void connectionStatusUpdate(@Nullable MySensorsAbstractConnection connection, boolean connected) {
        if (myNetSanCheck != null) {
            if (connected) {
                myNetSanCheck.start();
            } else {
                myNetSanCheck.stop();
            }
        }
        logger.debug("MySensorsGateway connection status update -connected: {}", connected);
        handleBridgeStatusUpdate(connected);
    }

    public MySensorsGatewayConfig getConfiguration() {
        return myConf;
    }

    private void handleBridgeStatusUpdate(boolean connected) {
        synchronized (nodeMap) {
            for (Integer i : nodeMap.keySet()) {
                MySensorsNode node = nodeMap.get(i);
                node.setReachable(connected);
                myEventRegister.notifyNodeReachEvent(node, connected);
            }
        }
    }

    /**
     * Handle the incoming/outgoing message from serial
     *
     * @param msg the incoming/outgoing message
     * @return true if ,and only if:
     *         -the message is propagated to one of the defined node or
     *         -message arrives from a device new device in the network or
     *         -message is REQ type and variable is defined for it
     */
    private boolean handleIncomingMessage(MySensorsMessage msg) {
        if (MySensorsNode.isValidNodeId(msg.getNodeId()) && MySensorsChild.isValidChildId(msg.getChildId())) {
            if (msg.getDirection() == MySensorsMessageDirection.INCOMING) {
                updateReachable(msg);
                updateLastUpdateFromMessage(msg);

                if (msg.getMsgType() == MySensorsMessageType.INTERNAL) {
                    return handleInternalMessage(msg);
                } else if (msg.getMsgType() == MySensorsMessageType.SET
                        || msg.getMsgType() == MySensorsMessageType.REQ) {
                    return handleSetReqMessage(msg, true);
                } else if (msg.getMsgType() == MySensorsMessageType.PRESENTATION) {
                    return handlePresentationMessage(msg);
                } else {
                    return isNewDevice(msg);
                }
            } else {
                logger.warn("Cannot handle this message, direction MYSENSORS_MSG_DIRECTION_OUTGOING");
            }
        }
        return false;
    }

    private void handleOutgoingMessage(MySensorsMessage msg) {
        if (MySensorsNode.isValidNodeId(msg.getNodeId()) && MySensorsChild.isValidChildId(msg.getChildId())) {
            if (msg.getDirection() == MySensorsMessageDirection.OUTGOING) {
                if (msg.getMsgType() == MySensorsMessageType.SET) {
                    handleSetReqMessage(msg, false);
                }
            } else {
                logger.warn("Cannot handle this message, direction MYSENSORS_MSG_DIRECTION_INCOMING");
            }
        }
    }

    private void updateReachable(MySensorsMessage msg) {
        @Nullable
        MySensorsNode node = getNode(msg.getNodeId());
        if (node != null && !node.isReachable()) {
            logger.info("Node {} available again!", node.getNodeId());
            node.setReachable(true);
            myEventRegister.notifyNodeReachEvent(node, true);
        }
    }

    private boolean isNewDevice(MySensorsMessage msg) {
        @Nullable
        MySensorsNode node = getNode(msg.getNodeId());

        if (node == null) {
            logger.debug("Node {} not present, send new node discovered event", msg.getNodeId());

            node = new MySensorsNode(msg.getNodeId());

            addNode(node);
            myEventRegister.notifyNewNodeDiscovered(node, null);
            return true;
        }
        return false;
    }

    private boolean handlePresentationMessage(MySensorsMessage msg) {
        boolean insertNode = false;

        @Nullable
        MySensorsNode node = getNode(msg.getNodeId());

        @Nullable
        MySensorsChild child = getChild(msg.getNodeId(), msg.getChildId());

        logger.debug("Presentation Message received");

        if (child == null) {
            if (node == null) {
                node = new MySensorsNode(msg.getNodeId());
                insertNode = true;
            }

            child = MySensorsChild.fromPresentation(msg.getSubType(), msg.getChildId());
            if (child != null) {
                node.addChild(child);

                if (insertNode) {
                    addNode(node);
                }

                myEventRegister.notifyNewNodeDiscovered(node, child);
                return true;
            } else {
                logger.warn("Could not present child because MySensorsChild from presentation was null");
            }
        } else {
            logger.warn("Presented child is already present in gateway");
        }

        return false;
    }

    private boolean handleSetReqMessage(MySensorsMessage msg, boolean dispatchUpdate) {
        @Nullable
        MySensorsNode node = getNode(msg.getNodeId());
        if (node != null) {
            logger.debug("Node {} found in gateway", msg.getNodeId());

            @Nullable
            MySensorsChild child = node.getChild(msg.getChildId());
            if (child != null) {
                logger.debug("Child {} found in node {}", msg.getChildId(), msg.getNodeId());
                @Nullable
                MySensorsVariable variable = child.getVariable(msg.getSubType());

                if (variable != null) {
                    if (msg.isSetMessage()) {
                        if (node.isReachable()) {
                            logger.trace("Variable {}({}) found in child, pre-update value: {}",
                                    variable.getClass().getSimpleName(), variable.getType(), variable.getValue());
                            variable.setValue(msg);
                            logger.trace("Variable {}({}) found in child, post-update value: {}",
                                    variable.getClass().getSimpleName(), variable.getType(), variable.getValue());

                            if (dispatchUpdate) {
                                myEventRegister.notifyNodeUpdateEvent(node, child, variable,
                                        MySensorsNodeUpdateEventType.UPDATE);
                            }
                        } else {
                            logger.warn("Could not set value to node {} if not reachable", node.getNodeId());
                        }
                    } else {
                        @Nullable
                        String value = variable.getValue();
                        logger.debug("Request received!");
                        msg.setMsgType(MySensorsMessageType.SET);
                        msg.setMsg(Objects.requireNonNullElse(value, "0"));
                        if (myCon != null)
                            myCon.sendMessage(msg);
                    }
                    return true;
                } else {
                    logger.warn("Variable {} not present", msg.getSubType());
                }
            } else {
                logger.warn("Child:{} not found on node:{}, cannot handle message", msg.getChildId(), msg.getNodeId());
            }
        }
        return false;
    }

    private boolean handleInternalMessage(MySensorsMessage msg) {
        @Nullable
        MySensorsNode node = getNode(msg.getNodeId());
        if (node != null) {
            if (msg.getSubType() == MySensorsMessageSubType.I_BATTERY_LEVEL) {
                node.setBatteryPercent(Integer.parseInt(msg.getMsg()));
                logger.debug("Battery percent for node {} update to: {}%", node.getNodeId(), node.getBatteryPercent());
                myEventRegister.notifyNodeUpdateEvent(node, null, null, MySensorsNodeUpdateEventType.BATTERY);
                return true;
            }
        }
        return false;
    }

    private void updateLastUpdateFromMessage(@Nullable MySensorsMessage msg) {
        Date now = new Date();

        // Last update updated only for incoming message
        if (msg != null) {
            @Nullable
            MySensorsNode node = getNode(msg.getNodeId());

            if (node != null) {
                node.setLastUpdate(now);
                @Nullable
                MySensorsChild child = getChild(msg.getNodeId(), msg.getChildId());

                if (child != null) {
                    child.setLastUpdate(now);

                    @Nullable
                    MySensorsVariable var = getVariable(msg.getNodeId(), msg.getChildId(), msg.getSubType());

                    if (var != null) {
                        var.setLastUpdate(now);
                    }
                }
            }
        }
    }

    private void handleSpecialMessageEvent(MySensorsMessage msg) {
        // Is this an I_CONFIG message?
        if (msg.isIConfigMessage()) {
            answerIConfigMessage(msg);
        }

        // Is this an I_TIME message?
        if (msg.isITimeMessage()) {
            answerITimeMessage(msg);
        }

        // Requesting ID
        if (msg.isIdRequestMessage()) {
            answerIDRequest();
        }
    }

    /**
     * Answer to I_TIME message for gateway time request from sensor
     *
     * @param msg, the incoming I_TIME message from sensor
     */
    private void answerITimeMessage(MySensorsMessage msg) {
        logger.info("I_TIME request received from {}, answering...", msg.getNodeId());

        long currentTime = System.currentTimeMillis();

        String time = Long.toString((currentTime + TimeZone.getDefault().getOffset(currentTime)) / 1000);

        MySensorsMessage newMsg = new MySensorsMessage(msg.getNodeId(), msg.getChildId(), MySensorsMessageType.INTERNAL,
                MySensorsMessageAck.FALSE, false, MySensorsMessageSubType.I_TIME, time);

        if (myCon != null)
            myCon.sendMessage(newMsg);
    }

    /**
     * Answer to I_CONFIG message for imperial/metric request from sensor
     *
     * @param msg, the incoming I_CONFIG message from sensor
     */
    private void answerIConfigMessage(MySensorsMessage msg) {
        if (myCon == null) {
            logger.warn("Connection or Configuration is null, skipping I_CONFIG");
            return;
        }
        boolean imperial = myConf.getImperial();
        String iConfig = imperial ? "I" : "M";

        logger.debug("I_CONFIG request received from {}, answering: (is imperial?){}", iConfig, imperial);

        MySensorsMessage newMsg = new MySensorsMessage(msg.getNodeId(), msg.getChildId(), MySensorsMessageType.INTERNAL,
                MySensorsMessageAck.FALSE, false, MySensorsMessageSubType.I_CONFIG, iConfig);
        myCon.sendMessage(newMsg);
    }

    /**
     * If an ID-Request from a sensor is received the controller will send an id to the sensor
     */
    private void answerIDRequest() {
        logger.info("ID Request received");

        try {
            int newId = reserveId();
            logger.info("New Node in the MySensors network has requested an ID. ID is: {}", newId);
            MySensorsMessage newMsg = new MySensorsMessage(MySensorsNode.MYSENSORS_NODE_ID_RESERVED_255,
                    MySensorsChild.MYSENSORS_CHILD_ID_RESERVED_255, MySensorsMessageType.INTERNAL,
                    MySensorsMessageAck.FALSE, false, MySensorsMessageSubType.I_ID_RESPONSE, newId + "");
            if (myCon != null) {
                myCon.sendMessage(newMsg);
            } else {
                logger.warn("Skipping answerIDRequest, connection is null");
            }
        } catch (NoMoreIdsException e) {
            logger.error("No more IDs available for this node, you could try cleaning cache file");
        }
    }
}
