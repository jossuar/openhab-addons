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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.config.MySensorsBridgeConfiguration;
import org.openhab.binding.mysensors.discovery.MySensorsDiscoveryService;
import org.openhab.binding.mysensors.factory.MySensorsCacheFactory;
import org.openhab.binding.mysensors.internal.event.MySensorsGatewayEventListener;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGateway;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGatewayConfig;
import org.openhab.binding.mysensors.internal.gateway.MySensorsGatewayType;
import org.openhab.binding.mysensors.internal.protocol.MySensorsAbstractConnection;
import org.openhab.binding.mysensors.internal.sensors.MySensorsChild;
import org.openhab.binding.mysensors.internal.sensors.MySensorsNode;
import org.openhab.core.OpenHAB;
import org.openhab.core.io.transport.serial.SerialPortManager;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

/**
 * MySensorsBridgeHandler is used to initialize a new bridge (in MySensors: Gateway)
 * The sensors are connected via the gateway/bridge to the controller
 *
 * @author Tim Oberföll - Initial contribution
 *
 */
@NonNullByDefault
public class MySensorsBridgeHandler extends BaseBridgeHandler implements MySensorsGatewayEventListener {

    private final Logger logger = LoggerFactory.getLogger(MySensorsBridgeHandler.class);

    // Gateway instance
    private @Nullable MySensorsGateway myGateway;

    // Configuration from thing file
    private MySensorsBridgeConfiguration myBridgeConfiguration;

    private final MySensorsCacheFactory cacheFactory;

    private @Nullable MySensorsDiscoveryService discoveryService;

    private final SerialPortManager serialPortManager;

    public void setDiscoveryService(MySensorsDiscoveryService discoveryService) {
        this.discoveryService = discoveryService;
    }

    public MySensorsBridgeHandler(Bridge bridge, SerialPortManager serialPortManager) {
        super(bridge);
        cacheFactory = new MySensorsCacheFactory(OpenHAB.getUserDataFolder());
        this.serialPortManager = serialPortManager;
        myBridgeConfiguration = new MySensorsBridgeConfiguration();
    }

    @Override
    public void initialize() {
        logger.debug("Initialization of the MySensors bridge {}", getThing().getUID());

        myBridgeConfiguration = getConfigAs(MySensorsBridgeConfiguration.class);

        myGateway = new MySensorsGateway(loadCacheFile(), serialPortManager);

        if (myGateway.setup(openhabToMySensorsGatewayConfig(myBridgeConfiguration, getThing().getThingTypeUID()))) {
            myGateway.startup();

            myGateway.addEventListener(this);

            logger.debug("Initialization of the MySensors bridge {} DONE!", getThing().getUID());

            updateStatus(ThingStatus.ONLINE);
        } else {
            logger.error("Failed to initialize MySensors bridge {}", getThing().getUID());
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing of the MySensors bridge {}", getThing().getUID());
        updateStatus(ThingStatus.OFFLINE);
        if (myGateway != null) {
            myGateway.removeEventListener(this);
            myGateway.shutdown();
        }

        super.dispose();
    }

    @Override
    public void thingUpdated(Thing thing) {
        logger.debug("MySensorsBridgeHandler thing updated {}", thing.getUID());
        super.thingUpdated(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    /**
     * Getter for the connection to the MySensors bridge / gateway.
     * Used for receiving (register handler) and sending of messages.
     *
     * @return Connection to the MySensors bridge / gateway.
     */
    @Nullable
    public MySensorsGateway getMySensorsGateway() {
        return myGateway;
    }

    @Override
    public void connectionStatusUpdate(@Nullable MySensorsAbstractConnection connection, boolean connected) {
        if (connected) {
            updateStatus(ThingStatus.ONLINE);
            if (discoveryService != null)
                discoveryService.activate();
        } else {
            updateStatus(ThingStatus.OFFLINE);
            if (discoveryService != null)
                discoveryService.deactivate();
        }
        logger.debug("Connection status {} updated to {}", getThing().getUID(), connected);
        updateCacheFile();
    }

    @Override
    public void nodeIdReservationDone(@Nullable Integer reservedId) {
        updateCacheFile();
    }

    @Override
    public void newNodeDiscovered(@Nullable MySensorsNode node, @Nullable MySensorsChild child) {
        updateCacheFile();
    }

    @Override
    public void handleConfigurationUpdate(Map<String, Object> configurationParameters) {
        logger.debug("Configuration update for bridge: {}", configurationParameters);
        super.handleConfigurationUpdate(configurationParameters);
    }

    private void updateCacheFile() {
        if (myGateway == null)
            return;

        List<Integer> givenIds = myGateway.getGivenIds();

        String cacheFileName = MySensorsCacheFactory.GIVEN_IDS_CACHE_FILE + "_"
                + getThing().getUID().toString().replace(':', '_');

        cacheFactory.writeCache(cacheFileName, givenIds.toArray(new Integer[] {}), Integer[].class);
    }

    private Map<Integer, MySensorsNode> loadCacheFile() {
        Map<Integer, MySensorsNode> nodes = new HashMap<>();

        String cacheFileName = MySensorsCacheFactory.GIVEN_IDS_CACHE_FILE + "_"
                + getThing().getUID().toString().replace(':', '_');
        List<Integer> givenIds = cacheFactory.readCache(cacheFileName, new ArrayList<>(),
                new TypeToken<ArrayList<Integer>>() {
                }.getType());

        for (Integer i : givenIds) {
            nodes.put(i, new MySensorsNode(i));
        }

        return nodes;
    }

    private MySensorsGatewayConfig openhabToMySensorsGatewayConfig(MySensorsBridgeConfiguration conf,
            ThingTypeUID bridgeuid) {
        MySensorsGatewayConfig gatewayConfig = new MySensorsGatewayConfig();

        if (bridgeuid.equals(THING_TYPE_BRIDGE_SER)) {
            gatewayConfig.setGatewayType(MySensorsGatewayType.SERIAL);
            gatewayConfig.setBaudRate(conf.baudRate);
            gatewayConfig.setSerialPort(conf.serialPort);
            gatewayConfig.setHardReset(conf.hardReset);
        } else if (bridgeuid.equals(THING_TYPE_BRIDGE_ETH)) {
            gatewayConfig.setGatewayType(MySensorsGatewayType.IP);
            gatewayConfig.setIpAddress(conf.ipAddress);
            gatewayConfig.setTcpPort(conf.tcpPort);
        } else if (bridgeuid.equals(THING_TYPE_BRIDGE_MQTT)) {
            gatewayConfig.setGatewayType(MySensorsGatewayType.MQTT);
            gatewayConfig.setBrokerName(conf.brokerName);
            gatewayConfig.setTopicPublish(conf.topicPublish);
            gatewayConfig.setTopicSubscribe(conf.topicSubscribe);
        } else {
            throw new IllegalArgumentException("BridgeUID is unknown: " + bridgeuid);
        }

        gatewayConfig.setSendDelay(conf.sendDelay);
        gatewayConfig.setEnableNetworkSanCheck(conf.networkSanCheckEnabled);
        gatewayConfig.setImperial(conf.imperial);
        gatewayConfig.setStartupCheck(conf.startupCheckEnabled);
        gatewayConfig.setSanityCheckerInterval(conf.networkSanCheckInterval);
        gatewayConfig.setSanCheckConnectionFailAttempts(conf.networkSanCheckConnectionFailAttempts);
        gatewayConfig.setSanCheckSendHeartbeat(conf.networkSanCheckSendHeartbeat);
        gatewayConfig.setSanCheckSendHeartbeatFailAttempts(conf.networkSanCheckSendHeartbeatFailAttempts);

        return gatewayConfig;
    }
}
