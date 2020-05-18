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
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

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

import com.nextian.ipmi.api.async.ConnectionHandle;
import com.nextian.ipmi.api.sync.IpmiConnector;
import com.nextian.ipmi.coding.commands.IpmiVersion;
import com.nextian.ipmi.coding.commands.PrivilegeLevel;
import com.nextian.ipmi.coding.commands.chassis.GetChassisStatus;
import com.nextian.ipmi.coding.commands.chassis.GetChassisStatusResponseData;
import com.nextian.ipmi.coding.commands.sdr.GetSdr;
import com.nextian.ipmi.coding.commands.sdr.GetSdrRepositoryInfo;
import com.nextian.ipmi.coding.commands.sdr.GetSdrRepositoryInfoResponseData;
import com.nextian.ipmi.coding.commands.sdr.GetSdrResponseData;
import com.nextian.ipmi.coding.commands.sdr.GetSensorReading;
import com.nextian.ipmi.coding.commands.sdr.GetSensorReadingResponseData;
import com.nextian.ipmi.coding.commands.sdr.ReserveSdrRepository;
import com.nextian.ipmi.coding.commands.sdr.ReserveSdrRepositoryResponseData;
import com.nextian.ipmi.coding.commands.sdr.record.FullSensorRecord;
import com.nextian.ipmi.coding.commands.sdr.record.SensorRecord;
import com.nextian.ipmi.coding.payload.lan.IPMIException;
import com.nextian.ipmi.coding.protocol.AuthenticationType;
import com.nextian.ipmi.coding.security.CipherSuite;

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
        boolean thingReachable = true;

        try {
            IpmiConfig config = this.config;
            if (config != null) {
                IpmiConnector connector = new IpmiConnector(config.port);
                ConnectionHandle handle = connector.createConnection(InetAddress.getByName(config.host));

                // Get cipher suite and use it for the session.
                CipherSuite cs = connector.getDefaultCipherSuite(handle);
                System.out.println("Cipher suite selected");

                // Pass cipher suite and privilege level to the remote host.
                // From now on, the connection handle will contain ths information.
                connector.getChannelAuthenticationCapabilities(handle, cs, PrivilegeLevel.Administrator);
                System.out.println("Channel authentication capabilities received");

                // Start the session, provide username and password, and optionally the
                // BMC key (only if the remote host has two-key authentication enabled,
                // otherwise this parameter should be null).
                connector.openSession(handle, config.user, config.password, null);
                System.out.println("Session open");

                // Send a message and read the response
                GetChassisStatusResponseData chassisData = (GetChassisStatusResponseData) connector.sendMessage(handle,
                        new GetChassisStatus(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus));

                System.out.println("Received GetChassisStatus answer");
                System.out.println("System power state is " + (chassisData.isPowerOn() ? "up" : "down"));

                GetSdrRepositoryInfoResponseData sdrRepoData = (GetSdrRepositoryInfoResponseData) connector.sendMessage(
                        handle, new GetSdrRepositoryInfo(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus));

                System.out.println("Received GetSdrRepositoryInfo answer");
                System.out.println("Sdr Version is " + sdrRepoData.getSdrVersion());

                ReserveSdrRepositoryResponseData reserveData = (ReserveSdrRepositoryResponseData) connector.sendMessage(
                        handle, new ReserveSdrRepository(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus));
                int reservationId = reserveData.getReservationId();
                System.out.println("Received ReserveSdrRepository answer");
                System.out.println("Reservation Id is " + reservationId);

                GetSdrResponseData data1 = null;
                int offset = 0;
                int bytesToRead = 0x10;
                ByteBuffer bb = ByteBuffer.allocate(1000);
                try {
                    do {
                        data1 = (GetSdrResponseData) connector.sendMessage(handle, new GetSdr(IpmiVersion.V20, cs,
                                AuthenticationType.RMCPPlus, reservationId, 0, offset, bytesToRead));
                        offset += bytesToRead;

                        bb.put(data1.getSensorRecordData());
                    } while (bytesToRead == data1.getSensorRecordData().length);
                } catch (IPMIException e) {
                    System.out.println(
                            "code: " + e.getCompletionCode().getCode() + ", " + e.getCompletionCode().getMessage());
                }

                byte[] dst = new byte[bb.position()];
                bb.flip();
                bb.get(dst);

                SensorRecord record = SensorRecord.populateSensorRecord(dst);

                if (record instanceof FullSensorRecord) {
                    FullSensorRecord fr = (FullSensorRecord) record;

                    System.out.println("Sensor Type: " + fr.getSensorType().toString());
                    System.out.println("Address Type: " + fr.getAddressType().toString());
                    System.out.println("Rate Unit: " + fr.getRateUnit().toString());
                    System.out.println("Sensor Direction: " + fr.getSensorDirection());
                    System.out.println("Sensor Base Unit: " + fr.getSensorBaseUnit().toString());

                    System.out.println("fr.getUpperNonCriticalThreshold(): " + fr.getUpperNonCriticalThreshold());
                    System.out.println("fr.getUpperCriticalThreshold(): " + fr.getUpperCriticalThreshold());
                    System.out.println("fr.getUpperNonRecoverableThreshold(): " + fr.getUpperNonRecoverableThreshold());
                    System.out.println("fr.getLowerNonRecoverableThreshold(): " + fr.getLowerNonRecoverableThreshold());
                    System.out.println("fr.getLowerCriticalThreshold(): " + fr.getLowerCriticalThreshold());
                    System.out.println("fr.getLowerNonCriticalThreshold(): " + fr.getLowerNonCriticalThreshold());
                    System.out.println("fr.getNormalMinimum(): " + fr.getNormalMinimum());
                    System.out.println("fr.getNormalMaximum(): " + fr.getNormalMaximum());
                    System.out.println("fr.getNominalReading(): " + fr.getNominalReading());
                    byte sensor = fr.getSensorNumber();
                    GetSensorReadingResponseData readingData = (GetSensorReadingResponseData) connector.sendMessage(
                            handle, new GetSensorReading(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus, sensor));

                    System.out.println("Plain Sensor Reading: " + readingData.getPlainSensorReading());
                    System.out.println("Sensor State is " + readingData.getSensorState());
                }

                // Close the session
                connector.closeSession(handle);
                logger.debug("End initializing!");

                if (thingReachable) {
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE);
                }
            }
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE);
            return;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
