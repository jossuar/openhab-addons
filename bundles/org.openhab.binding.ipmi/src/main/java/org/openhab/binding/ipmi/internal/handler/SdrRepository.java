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
import java.util.HashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nextian.ipmi.api.async.ConnectionHandle;
import com.nextian.ipmi.api.sync.IpmiConnector;
import com.nextian.ipmi.coding.commands.IpmiVersion;
import com.nextian.ipmi.coding.commands.PrivilegeLevel;
import com.nextian.ipmi.coding.commands.sdr.GetSdr;
import com.nextian.ipmi.coding.commands.sdr.GetSdrResponseData;
import com.nextian.ipmi.coding.commands.sdr.ReserveSdrRepository;
import com.nextian.ipmi.coding.commands.sdr.ReserveSdrRepositoryResponseData;
import com.nextian.ipmi.coding.commands.sdr.record.CompactSensorRecord;
import com.nextian.ipmi.coding.commands.sdr.record.DeviceRelativeEntityAssiciationRecord;
import com.nextian.ipmi.coding.commands.sdr.record.EntityAssociationRecord;
import com.nextian.ipmi.coding.commands.sdr.record.EventOnlyRecord;
import com.nextian.ipmi.coding.commands.sdr.record.FruDeviceLocatorRecord;
import com.nextian.ipmi.coding.commands.sdr.record.FullSensorRecord;
import com.nextian.ipmi.coding.commands.sdr.record.GenericDeviceLocatorRecord;
import com.nextian.ipmi.coding.commands.sdr.record.ManagementControllerConfirmationRecord;
import com.nextian.ipmi.coding.commands.sdr.record.ManagementControllerDeviceLocatorRecord;
import com.nextian.ipmi.coding.commands.sdr.record.OemRecord;
import com.nextian.ipmi.coding.commands.sdr.record.SensorRecord;
import com.nextian.ipmi.coding.payload.lan.IPMIException;
import com.nextian.ipmi.coding.protocol.AuthenticationType;
import com.nextian.ipmi.coding.security.CipherSuite;
import com.nextian.ipmi.connection.ConnectionException;

/**
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class SdrRepository {
    private final Logger logger = LoggerFactory.getLogger(SdrRepository.class);
    private final String host;
    private final int port;

    private final IpmiConnector connector;
    private final ConnectionHandle handle;
    private final CipherSuite cs;

    private final HashMap<Integer, Sdr> repository = new HashMap<Integer, Sdr>();

    public SdrRepository(String host, int port) throws IOException, ConnectionException, InterruptedException {
        this.host = host;
        this.port = port;

        connector = new IpmiConnector(port);
        handle = connector.createConnection(InetAddress.getByName(host));
        cs = connector.getDefaultCipherSuite(handle);
        connector.getChannelAuthenticationCapabilities(handle, cs, PrivilegeLevel.Administrator);
    }

    public void fillSdrRepository() throws IllegalArgumentException, InterruptedException, IPMIException, IOException {
        int recordId = 0;

        ReserveSdrRepositoryResponseData reserveData = (ReserveSdrRepositoryResponseData) connector.sendMessage(handle,
                new ReserveSdrRepository(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus));
        int reservationId = reserveData.getReservationId();

        do {
            recordId = readSdr(reservationId, recordId);
        } while (recordId != 0xffff);

        return;
    }

    private class SdrRecord {
        public final byte[] buffer;
        public final int nextRecordId;

        public SdrRecord(byte[] buffer, int nextRecordId) {
            this.buffer = buffer;
            this.nextRecordId = nextRecordId;
        }
    }

    private SdrRecord readSdrBuffer(int reservationId, int recordId)
            throws IllegalArgumentException, InterruptedException, IOException {
        GetSdrResponseData data = null;
        int offset = 0;
        int bytesToRead = 0xff;
        int nextRecordId = 0xffff;
        boolean retry = true;

        ByteBuffer bb = ByteBuffer.allocate(1000);

        do {
            try {
                do {
                    data = (GetSdrResponseData) connector.sendMessage(handle, new GetSdr(IpmiVersion.V20, cs,
                            AuthenticationType.RMCPPlus, reservationId, recordId, offset, bytesToRead));
                    offset += bytesToRead;

                    bb.put(data.getSensorRecordData());
                } while (bytesToRead == 0xff || bytesToRead == data.getSensorRecordData().length);

                nextRecordId = data.getNextRecordId();
                retry = false;
            } catch (IPMIException e) {
                offset = 0;
                bytesToRead = bytesToRead / 2;

                if (bytesToRead == 0) {
                    retry = false;
                }
                System.out.println(
                        "code: " + e.getCompletionCode().getCode() + ", " + e.getCompletionCode().getMessage());
            }
        } while (retry);

        // Get Sdr byte array
        byte[] buffer = new byte[bb.position()];
        bb.flip();
        bb.get(buffer);

        return new SdrRecord(buffer, nextRecordId);
    }

    private int readSdr(int reservationId, int recordId)
            throws IllegalArgumentException, InterruptedException, IOException {
        SdrRecord rec = readSdrBuffer(reservationId, recordId);

        // Build the sensor record
        SensorRecord record = SensorRecord.populateSensorRecord(rec.buffer);

        if (record instanceof CompactSensorRecord) {
            logger.debug("CompactSensorRecord not supported");
        } else if (record instanceof DeviceRelativeEntityAssiciationRecord) {
            logger.debug("DeviceRelativeEntityAssiciationRecord not supported");
        } else if (record instanceof EntityAssociationRecord) {
            logger.debug("EntityAssociationRecord not supported");
        } else if (record instanceof EventOnlyRecord) {
            logger.debug("EventOnlyRecord not supported");
        } else if (record instanceof FruDeviceLocatorRecord) {
            logger.debug("FruDeviceLocatorRecord not supported");
        } else if (record instanceof GenericDeviceLocatorRecord) {
            logger.debug("GenericDeviceLocatorRecord not supported");
        } else if (record instanceof ManagementControllerConfirmationRecord) {
            logger.debug("ManagementControllerConfirmationRecord not supported");
        } else if (record instanceof ManagementControllerDeviceLocatorRecord) {
            logger.debug("ManagementControllerDeviceLocatorRecord not supported");
        } else if (record instanceof OemRecord) {
            logger.debug("OemRecord not supported");
        } else if (record instanceof FullSensorRecord) {
            FullSensorRecord fr = (FullSensorRecord) record;

            Sdr sdr = new Sdr(fr.getSensorNumber(), fr.getSensorType(), fr.getAddressType(), fr.getRateUnit(),
                    fr.getSensorDirection(), fr.getSensorBaseUnit().toString(), fr.getUpperNonCriticalThreshold(),
                    fr.getUpperCriticalThreshold(), fr.getUpperNonRecoverableThreshold(),
                    fr.getLowerNonRecoverableThreshold(), fr.getLowerCriticalThreshold(),
                    fr.getLowerNonCriticalThreshold(), fr.getNormalMinimum(), fr.getNormalMaximum(),
                    fr.getNominalReading());

            repository.put((int) fr.getSensorNumber(), sdr);
        }

        return rec.nextRecordId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public HashMap<Integer, Sdr> getRepository() {
        return repository;
    }
}
