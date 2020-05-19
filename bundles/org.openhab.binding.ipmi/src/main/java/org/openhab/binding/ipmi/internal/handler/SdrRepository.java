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
    private final IpmiConnector connector;
    private final ConnectionHandle handle;
    private final CipherSuite cs;
    private final HashMap<Integer, Sdr> repository = new HashMap<Integer, Sdr>();
    private int bytesToRead = 0xff;

    public SdrRepository(String host, int port, String user, String password)
            throws IOException, ConnectionException, InterruptedException, IllegalArgumentException, IPMIException {
        try {
            connector = new IpmiConnector(port);
            handle = connector.createConnection(InetAddress.getByName(host));
            cs = connector.getDefaultCipherSuite(handle);
            connector.getChannelAuthenticationCapabilities(handle, cs, PrivilegeLevel.Administrator);
            connector.openSession(handle, user, password, null);
            logger.warn("Session open");

            fillSdrRepository();
        } catch (IPMIException | IllegalArgumentException | InterruptedException | IOException
                | ConnectionException e) {
            logger.warn("Cannot read repository.", e);
            throw e;
        }
    }

    public HashMap<Integer, Sdr> getRepository() {
        return repository;
    }

    private void fillSdrRepository() throws IllegalArgumentException, InterruptedException, IPMIException, IOException {
        int recordId = 0;

        ReserveSdrRepositoryResponseData reserveData = (ReserveSdrRepositoryResponseData) connector.sendMessage(handle,
                new ReserveSdrRepository(IpmiVersion.V20, cs, AuthenticationType.RMCPPlus));
        int reservationId = reserveData.getReservationId();

        do {
            recordId = readSdr(reservationId, recordId);
        } while (recordId != 0xffff);

        return;
    }

    private SdrRecord readSdrBuffer(int reservationId, int recordId)
            throws IllegalArgumentException, InterruptedException, IOException {
        GetSdrResponseData data = null;
        int offset = 0;
        int nextRecordId = 0xffff;
        boolean retry = true;

        ByteBuffer bb = ByteBuffer.allocate(1000);

        do {
            try {
                do {
                    data = (GetSdrResponseData) connector.sendMessage(handle, new GetSdr(IpmiVersion.V20, cs,
                            AuthenticationType.RMCPPlus, reservationId, recordId, offset, bytesToRead));
                    offset += bytesToRead;

                    nextRecordId = data.getNextRecordId();
                    bb.put(data.getSensorRecordData());
                } while ((bytesToRead == 0xff && data.getSensorRecordData().length > 0)
                        || (bytesToRead != 0xff && data.getSensorRecordData().length == bytesToRead));

                retry = false;
            } catch (IPMIException e) {
                if (e.getCompletionCode().getCode() == 204) {
                    retry = false;
                } else {
                    offset = 0;
                    bytesToRead = bytesToRead / 2;

                    if (bytesToRead == 0) {
                        retry = false;
                    }
                    System.out.println(
                            "code: " + e.getCompletionCode().getCode() + ", " + e.getCompletionCode().getMessage());
                }
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

        addSensorRecord(rec.buffer);

        return rec.nextRecordId;
    }

    private void addSensorRecord(byte[] buffer) {
        // Build the sensor record
        SensorRecord record = SensorRecord.populateSensorRecord(buffer);

        if (record instanceof CompactSensorRecord) {
            logger.warn("CompactSensorRecord found");
            CompactSensorRecord csr = (CompactSensorRecord) record;

            Sdr sdr = new Sdr("CompactSensorRecord", csr.getSensorNumber(), csr.getSensorType(), csr.getName(),
                    csr.getAddressType(), csr.getRateUnit(), csr.getSensorDirection(),
                    csr.getSensorBaseUnit().toString(), -1, -1, -1, -1, -1, -1, -1, -1, -1);

            repository.put((int) csr.getSensorNumber(), sdr);
            logger.warn("CompactSensorRecord not supported");
        } else if (record instanceof DeviceRelativeEntityAssiciationRecord) {
            logger.warn("DeviceRelativeEntityAssiciationRecord not supported");
        } else if (record instanceof EntityAssociationRecord) {
            logger.warn("EntityAssociationRecord not supported");
        } else if (record instanceof EventOnlyRecord) {
            logger.warn("EventOnlyRecord not supported");
        } else if (record instanceof FruDeviceLocatorRecord) {
            logger.warn("FruDeviceLocatorRecord not supported");
        } else if (record instanceof GenericDeviceLocatorRecord) {
            logger.warn("GenericDeviceLocatorRecord not supported");
        } else if (record instanceof ManagementControllerConfirmationRecord) {
            logger.warn("ManagementControllerConfirmationRecord not supported");
        } else if (record instanceof ManagementControllerDeviceLocatorRecord) {
            logger.warn("ManagementControllerDeviceLocatorRecord not supported");
        } else if (record instanceof OemRecord) {
            logger.warn("OemRecord not supported");
        } else if (record instanceof FullSensorRecord) {
            logger.warn("FullSensorRecord found");
            FullSensorRecord fsr = (FullSensorRecord) record;

            Sdr sdr = new Sdr("FullSensorRecord", fsr.getSensorNumber(), fsr.getSensorType(), fsr.getName(),
                    fsr.getAddressType(), fsr.getRateUnit(), fsr.getSensorDirection(),
                    fsr.getSensorBaseUnit().toString(), fsr.getUpperNonCriticalThreshold(),
                    fsr.getUpperCriticalThreshold(), fsr.getUpperNonRecoverableThreshold(),
                    fsr.getLowerNonRecoverableThreshold(), fsr.getLowerCriticalThreshold(),
                    fsr.getLowerNonCriticalThreshold(), fsr.getNormalMinimum(), fsr.getNormalMaximum(),
                    fsr.getNominalReading());

            repository.put((int) fsr.getSensorNumber(), sdr);
        }
    }

    private class SdrRecord {
        public final byte[] buffer;
        public final int nextRecordId;

        public SdrRecord(byte[] buffer, int nextRecordId) {
            this.buffer = buffer;
            this.nextRecordId = nextRecordId;
        }
    }
}
