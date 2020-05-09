/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.binding.ipmi.internal.handler;

import java.io.IOException;
import java.net.InetAddress;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.ipmi.internal.config.IpmiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.veraxsystems.vxipmi.api.sync.IpmiConnector;

/**
 * {@link IpmiBridgeHandler} is the handler for a Homematic gateway and connects it to the framework.
 *
 * @author Gerhard Riegler - Initial contribution
 */
@NonNullByDefault
public class IpmiBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(IpmiBridgeHandler.class);

    private @Nullable IpmiConfig config;
    private @Nullable IpmiConnector connector;

    public IpmiBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(IpmiConfig.class);

        // The framework requires you to return from this method quickly. Also, before leaving this method a thing
        // status from one of ONLINE, OFFLINE or UNKNOWN must be set. This might already be the real thing status in
        // case you can decide it directly.
        // In case you can not decide the thing status directly (e.g. for long running connection handshake using WAN
        // access or similar) you should set status UNKNOWN here and then decide the real status asynchronously in the
        // background.

        // set the thing status to UNKNOWN temporarily and let the background task decide for the real status.
        // the framework is then able to reuse the resources from the thing handler initialization.
        // we set this upfront to reliably check status updates in unit tests.
        updateStatus(ThingStatus.UNKNOWN);

        // Example for background initialization:
        scheduler.execute(() -> {
            boolean thingReachable = true;

            try {
                IpmiConfig config = this.config;
                if (config != null) {
                    connector = new IpmiConnector(config.port);
                    connector.createConnection(InetAddress.getByName(config.host), config.port);
                }
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE);
                return;
            }

            // <background task with long running initialization here>
            // when done do:
            if (thingReachable) {
                updateStatus(ThingStatus.ONLINE);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        });

        logger.debug("Finished initializing!");

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    @Override
    public void dispose() {
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH == command) {
            logger.debug("Refreshing bridge '{}'", getThing().getUID().getId());
        }
    }
}
