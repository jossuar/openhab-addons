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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.ipmi.internal.config.IpmiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nextian.ipmi.coding.payload.lan.IPMIException;
import com.nextian.ipmi.connection.ConnectionException;

/**
 * {@link IpmiBridgeHandler} is the handler for an Ipmi gateway.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class IpmiBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(IpmiBridgeHandler.class);

    private @Nullable IpmiConfig config;
    private @Nullable Future<?> initializeFuture = null;

    public IpmiBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");
        config = getConfigAs(IpmiConfig.class);
        updateStatus(ThingStatus.UNKNOWN);

        initializeFuture = scheduler.submit(this::initializeInternal);

        logger.debug("Finished initializing!");
    }

    @Override
    public void dispose() {
        super.dispose();

        if (initializeFuture != null) {
            initializeFuture.cancel(true);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH == command) {
            logger.debug("Refreshing bridge '{}'", getThing().getUID().getId());
        }
    }

    private void initializeInternal() {
        try {
            IpmiConfig config = this.config;
            if (config != null) {
                SdrRepository repo = new SdrRepository(config.host, config.port, config.user, config.password);

                HashMap<Integer, Sdr> map = repo.getRepository();
                Set<Entry<Integer, Sdr>> entrySet = map.entrySet();

                // Obtaining an iterator for the entry set
                Iterator<Entry<Integer, Sdr>> it = entrySet.iterator();

                // Iterate through HashMap entries(Key-Value pairs)
                while (it.hasNext()) {
                    Entry<Integer, Sdr> me = it.next();
                    logger.warn("Entry [{}]: {}", me.getKey(), me.getValue());
                }
                updateStatus(ThingStatus.ONLINE);
            }
        } catch (IllegalArgumentException | IOException | InterruptedException | IPMIException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        } catch (ConnectionException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR);
        }
    }
}
