/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.risco.internal.message;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that represents the Risco Alarm Messages.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public abstract class RiscoMessage {
    private final Logger logger = LoggerFactory.getLogger(RiscoMessage.class);

    private final int commandId;
    private final String commandName;
    private final String[] commandValues;
    private final int indexFrom;
    private final int indexTo;
    private final String sign;

    private final byte[] encryptedMessage;
    private final byte[] decryptedMessage;

    public RiscoMessage(int commandId, String commandName, String sign, String[] commandValues, int indexFrom,
            int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {

        this.commandId = commandId;
        this.commandName = commandName;
        this.indexFrom = indexFrom;
        this.indexTo = indexTo;
        this.commandValues = commandValues;
        this.sign = sign;

        this.encryptedMessage = encryptedMessage;
        this.decryptedMessage = decryptedMessage;
    }

    /**
     * Returns a string representation of a RiscoMessage.
     *
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ENC: ").append(isEncrypted());
        sb.append(", MO: ").append(getMessageOrigin());
        sb.append(", CMD: ").append(commandName);
        // sb.append(", encryptedMessage: ").append(HexUtils.bytesToHex(encryptedMessage, " "));
        // sb.append(", MSG: ").append(commandId);

        return sb.toString();
    }

    public boolean isEncrypted() {
        return encryptedMessage.length > 1 && encryptedMessage[1] == 17;
    }

    public Integer getCommandId() {
        return commandId;
    }

    public MessageOrigin getMessageOrigin() {
        if (commandId >= 0 && commandId < 50) {
            return MessageOrigin.BINDING;
        } else if (commandId >= 50 && commandId < 100) {
            return MessageOrigin.PANEL;
        } else {
            return MessageOrigin.UNKNOWN;
        }
    }

    public byte[] getEncryptedMessage() {
        return encryptedMessage;
    }

    public byte[] getDecryptedMessage() {
        return decryptedMessage;
    }

    public String getCommandName() {
        return this.commandName;
    }

    public String[] getCommandValues() {
        return this.commandValues;
    }

    public boolean hasIndex() {
        return indexFrom != -1;
    }

    public boolean hasMultipleIndexes() {
        return indexFrom != indexTo;
    }

    public int getIndexFrom() {
        return indexFrom;
    }

    public int getIndexTo() {
        return indexTo;
    }

    public String getSign() {
        return sign;
    }

    public RiscoMessageType getMessageType() {
        RiscoMessageType mt = RiscoMessageType.valueOfMessage(commandName);
        if (mt == null) {
            mt = RiscoMessageType.UNKNOWN;
        }
        return mt;
    }

    public String getThingType() {
        return getMessageType().thingType;
    }

    public String[] getThingIds() {
        RiscoMessageType mt = getMessageType();
        List<String> ids = new ArrayList<String>();

        if (mt.hasIndex) {
            for (int i = getIndexFrom(); i <= getIndexTo(); i++) {
                ids.add(String.format(mt.thingIdFormat, i));
            }
        } else {
            ids.add(mt.thingIdFormat);
        }
        return ids.toArray(new String[0]);
    }

    public String getFullCommand() {
        StringBuilder sb = new StringBuilder();
        // sb.append(String.format("%02d", commandId));
        sb.append(getCommandName());
        sb.append(hasMultipleIndexes() ? "*" : "");
        sb.append(hasIndex() ? getIndexFrom() : "");
        sb.append(hasMultipleIndexes() ? ":" : "");
        sb.append(hasMultipleIndexes() ? getIndexTo() : "");
        sb.append(getSign());
        sb.append(String.join("\t", getCommandValues()));
        // sb.append(Character.toString((char) 23));

        return sb.toString();
    }

    abstract public ThingProperty[] getProperties();
}
