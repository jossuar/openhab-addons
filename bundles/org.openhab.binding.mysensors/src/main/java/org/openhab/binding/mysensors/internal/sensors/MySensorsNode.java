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
package org.openhab.binding.mysensors.internal.sensors;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.internal.exception.MergeException;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessage;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessageSubType;
import org.openhab.binding.mysensors.internal.protocol.message.MySensorsMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Characteristics of a thing/node are stored here:
 * - List of children
 * - Last update (DateTime) from the node
 * - is the child reachable?
 * - battery percent (if available)
 *
 * @author Andrea Cioni - Initial contribution
 *
 */
@NonNullByDefault
public class MySensorsNode {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    // Reserved ids
    public static final int MYSENSORS_NODE_ID_RESERVED_GATEWAY_0 = 0;
    public static final int MYSENSORS_NODE_ID_RESERVED_255 = 255;

    private final int nodeId;

    private MySensorsNodeConfig nodeConfig;

    private boolean reachable = true;

    private final Map<Integer, MySensorsChild> childMap;

    private Date lastUpdate;

    private int batteryPercent = 0;

    public MySensorsNode(int nodeId) {
        if (!isValidNodeId(nodeId)) {
            throw new IllegalArgumentException("Invalid node id supplied: " + nodeId);
        }
        this.nodeId = nodeId;
        this.childMap = new HashMap<>();
        this.lastUpdate = new Date(0);
        nodeConfig = new MySensorsNodeConfig();
    }

    public Map<Integer, MySensorsChild> getChildMap() {
        return childMap;
    }

    /**
     * Get node ID
     *
     * @return the ID of this node
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * Add a child not null child to child to this node
     *
     * @param child to add
     */
    public void addChild(MySensorsChild child) {
        synchronized (childMap) {
            childMap.put(child.getChildId(), child);
        }
    }

    /**
     * Get a child from a node
     *
     * @param childId the id of the child to get from this node
     * @return MySensorsChild for the given childId
     */
    @Nullable
    public MySensorsChild getChild(int childId) {
        return childMap.get(childId);
    }

    /**
     * Set node reachable status.
     *
     * @param reachable (true=yes,false=no)
     */
    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    /**
     * Check if this node is reachable
     *
     * @return true if this node is reachable
     */
    public boolean isReachable() {
        return reachable;
    }

    /**
     * Get battery percent of this node
     *
     * @return the battery percent
     */
    public int getBatteryPercent() {
        return batteryPercent;
    }

    /**
     * Set battery percent
     *
     * @param batteryPercent that will be set
     */
    public void setBatteryPercent(int batteryPercent) {
        this.batteryPercent = batteryPercent;
    }

    /**
     * Get last update
     *
     * @return the last update, 1970-01-01 00:00 means no update received
     */
    public Date getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Set last update
     *
     * @param lastUpdate to set date
     */
    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    /**
     * Get optional node configuration
     *
     * @return the Optional that could contains {@link MySensorsNodeConfig}
     */
    public MySensorsNodeConfig getNodeConfig() {
        return nodeConfig;
    }

    /**
     * Set configuration for node
     *
     * @param nodeConfig is a valid instance of {@link MySensorsNodeConfig}ß
     */
    public void setNodeConfig(MySensorsNodeConfig nodeConfig) {
        this.nodeConfig = nodeConfig;
    }

    /**
     * Merge to node into one.
     *
     * @param o object to merge
     *
     * @throws MergeException if try to merge to node with same child/children
     */
    public void merge(Object o) throws MergeException {
        if (!(o instanceof MySensorsNode)) {
            throw new MergeException("Cannot merge Non MySensorsNode into MySensorsNode");
        }
        MySensorsNode node = (MySensorsNode) o;
        nodeConfig.merge(node.nodeConfig);

        synchronized (childMap) {
            for (Integer i : node.childMap.keySet()) {
                MySensorsChild child = node.childMap.get(i);
                if (child == null)
                    continue;
                childMap.merge(i, child, (child1, child2) -> {
                    child1.merge(child2);
                    return child1;
                });
            }
        }
    }

    /**
     * Generate message from a state. This method doesn't update variable itself.
     * No check will be performed on value of state parameter
     *
     * @param childId id of the child the message is generated for.
     * @param subType subtype (humidity, temperature ...) the message is of.
     * @param state the new state that is send to the mysensors network.
     *
     * @return a non-null message ready to be sent if childId/type are available on this node
     *
     */
    @Nullable
    public MySensorsMessage updateVariableState(int childId, MySensorsMessageSubType subType, String state) {
        @Nullable
        MySensorsMessage msg = null;

        synchronized (childMap) {
            @Nullable
            MySensorsChild child = getChild(childId);
            if (child == null) {
                logger.warn("Cannot update variable state of null child:{}", childId);
                return null;
            }
            MySensorsChildConfig childConfig = child.getChildConfig();

            @Nullable
            MySensorsVariable var = child.getVariable(subType);
            if (var != null) {
                msg = new MySensorsMessage();

                // MySensors
                msg.setNodeId(nodeId);
                msg.setChildId(childId);
                msg.setMsgType(MySensorsMessageType.SET);
                msg.setSubType(subType);
                msg.setAck(childConfig.getRequestAck());
                msg.setMsg(state);
                msg.setRevert(childConfig.getRevertState());
                msg.setSmartSleep(childConfig.getSmartSleep());
            } else {
                throw new IllegalArgumentException("Cannot update variable state, variable type not found");
            }
        }
        return msg;
    }

    /**
     * Check if an integer is a valid node ID
     *
     * @param id to test
     *
     * @return true if ID is valid
     */
    public static boolean isValidNodeId(int id) {
        return (id >= MYSENSORS_NODE_ID_RESERVED_GATEWAY_0 && id < MYSENSORS_NODE_ID_RESERVED_255);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + childMap.hashCode();
        result = prime * result + nodeId;
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        MySensorsNode other = (MySensorsNode) obj;
        if (batteryPercent != other.batteryPercent) {
            return false;
        }
        if (!childMap.equals(other.childMap)) {
            return false;
        }
        if (!lastUpdate.equals(other.lastUpdate)) {
            return false;
        }
        if (nodeId != other.nodeId) {
            return false;
        }
        return reachable == other.reachable;
    }

    @Override
    public String toString() {
        return "MySensorsNode [nodeId=" + nodeId + ", childNumber=" + childMap.size() + ", chidldList=" + childMap
                + "]";
    }
}
