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

import org.eclipse.jdt.annotation.NonNullByDefault;

import com.nextian.ipmi.coding.commands.sdr.record.AddressType;
import com.nextian.ipmi.coding.commands.sdr.record.RateUnit;
import com.nextian.ipmi.coding.commands.sdr.record.SensorDirection;
import com.nextian.ipmi.coding.commands.sdr.record.SensorType;

/**
 * The {@link Sdr} is representing an SDR record
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class Sdr {
    private final String type;
    private final int sensorNumber;
    private final SensorType sensorType;
    private final String name;
    private final AddressType addressType;
    private final RateUnit rateUnit;
    private final SensorDirection sensorDirection;
    private final String sensorBaseUnit;

    private final double upperNonCriticalThreshold;
    private final double upperCriticalThreshold;

    private final double upperNonRecoverableThreshold;
    private final double lowerNonRecoverableThreshold;
    private final double lowerCriticalThreshold;
    private final double lowerNonCriticalThreshold;
    private final double normalMinimum;
    private final double normalMaximum;
    private final double nominalReading;

    public Sdr(String type, byte sensorNumber, SensorType sensorType, String name, AddressType addressType,
            RateUnit rateUnit, SensorDirection sensorDirection, String sensorBaseUnit, double upperNonCriticalThreshold,
            double upperCriticalThreshold, double upperNonRecoverableThreshold, double lowerNonRecoverableThreshold,
            double lowerCriticalThreshold, double lowerNonCriticalThreshold, double normalMinimum, double normalMaximum,
            double nominalReading) {
        this.type = type;
        this.sensorNumber = sensorNumber;
        this.sensorType = sensorType;
        this.name = name;
        this.addressType = addressType;
        this.rateUnit = rateUnit;
        this.sensorDirection = sensorDirection;
        this.sensorBaseUnit = sensorBaseUnit;

        this.upperNonCriticalThreshold = upperNonCriticalThreshold;
        this.upperCriticalThreshold = upperCriticalThreshold;
        this.upperNonRecoverableThreshold = upperNonRecoverableThreshold;
        this.lowerNonRecoverableThreshold = lowerNonRecoverableThreshold;
        this.lowerCriticalThreshold = lowerCriticalThreshold;
        this.lowerNonCriticalThreshold = lowerNonCriticalThreshold;
        this.normalMinimum = normalMinimum;
        this.normalMaximum = normalMaximum;
        this.nominalReading = nominalReading;
    }

    public String getType() {
        return type;
    }

    public int getSensorNumber() {
        return sensorNumber;
    }

    public SensorType getSensorType() {
        return sensorType;
    }

    public String getName() {
        return name;
    }

    public AddressType getAddressType() {
        return addressType;
    }

    public RateUnit getRateUnit() {
        return rateUnit;
    }

    public SensorDirection getSensorDirection() {
        return sensorDirection;
    }

    public String getSensorBaseUnit() {
        return sensorBaseUnit;
    }

    public double getUpperNonCriticalThreshold() {
        return upperNonCriticalThreshold;
    }

    public double getUpperCriticalThreshold() {
        return upperCriticalThreshold;
    }

    public double getUpperNonRecoverableThreshold() {
        return upperNonRecoverableThreshold;
    }

    public double getLowerNonRecoverableThreshold() {
        return lowerNonRecoverableThreshold;
    }

    public double getLowerCriticalThreshold() {
        return lowerCriticalThreshold;
    }

    public double getLowerNonCriticalThreshold() {
        return lowerNonCriticalThreshold;
    }

    public double getNormalMinimum() {
        return normalMinimum;
    }

    public double getNormalMaximum() {
        return normalMaximum;
    }

    public double getNominalReading() {
        return nominalReading;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.type).append(", ");
        sb.append(this.name).append(", ");
        sb.append(this.sensorNumber).append(", ");
        sb.append(this.sensorBaseUnit).append(", ");
        sb.append(this.getSensorType()).append(", ");
        sb.append(this.getSensorDirection());

        return sb.toString();
    }
}
