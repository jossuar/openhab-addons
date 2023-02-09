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

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class that represents the Risco Alarm Messages.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoMessage {
    private final Logger logger = LoggerFactory.getLogger(RiscoMessage.class);

    private final int panelId;
    private final String encoding;
    private final byte[] encryptedMessage;
    private final byte[] decryptedMessage;
    private final String stringMessage;
    private final int commandId;
    private final String command;
    private final String crcValue;

    private final String commandName;
    private final boolean multiIndex;
    private final int indexFrom;
    private final int indexTo;
    private final String[] commandValue;

    private final String ETB = Character.toString((char) 23);

    public RiscoMessage(int panelId, String encoding, byte[] encryptedMessage) {
        this.panelId = panelId;
        this.encoding = encoding;
        this.encryptedMessage = encryptedMessage;
        this.decryptedMessage = decrypt(encryptedMessage);
        this.stringMessage = bytesToString(decryptedMessage);

        if (stringMessage.startsWith("N") || stringMessage.startsWith("B")) {
            this.commandId = -1;
            this.command = stringMessage.substring(0, stringMessage.indexOf(ETB));
            this.crcValue = stringMessage.substring(stringMessage.indexOf(ETB) + 1);
        } else {
            this.commandId = Integer.parseInt(stringMessage.substring(0, 2), 10);
            this.command = stringMessage.substring(2, stringMessage.indexOf(ETB));
            this.crcValue = stringMessage.substring(stringMessage.indexOf(ETB) + 1);
        }

        // Computed information
        Object[] objs = splitCommand();
        this.commandName = (String) objs[0];
        this.multiIndex = (boolean) objs[1];
        this.indexFrom = (int) objs[2];
        this.indexTo = (int) objs[3];
        this.commandValue = (String[]) objs[4];
    }

    public RiscoMessage(int panelId, String encoding, Integer commandId, String command, Boolean encrypt) {
        this.panelId = panelId;
        this.encoding = encoding;
        this.commandId = commandId;
        this.command = command;

        // Add Cmd_Id to command and Separator character between Cmd and CRC value
        String cmd = String.format("%02d", commandId) + command + ETB;
        this.crcValue = this.calcCommandCRC(cmd);

        // Encrypt command string
        byte[] e = encrypt(cmd + crcValue, encrypt);

        // Build full encrypted byte[] message
        ByteArrayOutputStream encrypedOutputStream = new ByteArrayOutputStream();
        encrypedOutputStream.write(2);
        if (encrypt) {
            encrypedOutputStream.write(17);
        }
        encrypedOutputStream.write(e, 0, e.length);
        encrypedOutputStream.write(3);

        // Computed information
        this.encryptedMessage = encrypedOutputStream.toByteArray();
        this.decryptedMessage = decrypt(encryptedMessage);
        this.stringMessage = bytesToString(decryptedMessage);

        Object[] objs = splitCommand();
        this.commandName = (String) objs[0];
        this.multiIndex = (boolean) objs[1];
        this.indexFrom = (int) objs[2];
        this.indexTo = (int) objs[3];
        this.commandValue = (String[]) objs[4];
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
        sb.append(", MSG: ").append(command);
        sb.append(", VALUE: ").append(HexUtils.bytesToHex(command.getBytes(), " "));

        // sb.append(", MSG: ").append(commandId).append("-").append(this.crcValue).append("-").append(stringMessage);

        return sb.toString();
    }

    public boolean isEncrypted() {
        return encryptedMessage[1] == 17;
    }

    public Integer getCommandId() {
        return commandId;
    }

    public String getCommand() {
        return command;
    }

    public String getCrcValue() {
        return crcValue;
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

    public boolean isMultiIndex() {
        return multiIndex;
    }

    public int getIndexFrom() {
        return indexFrom;
    }

    public int getIndexTo() {
        return indexTo;
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

    public boolean isValidCRC() {
        if (crcValue.length() != 4) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            if (crcValue.charAt(i) > 127) {
                return false;
            }
        }

        String computedCrc = calcCommandCRC(command);
        boolean crcOK = crcValue.equals(computedCrc);

        logger.trace("Command[{}] crcOK:{}, Computed CRC: {}, Message CRC: {}", commandId, crcOK, computedCrc,
                crcValue);

        return crcOK;
    }

    private static Pattern NAME = Pattern.compile("^([A-Z]+)$");
    private static Pattern NAME_AND_INDEX = Pattern.compile("^([A-Z]+)(\\d+)$");
    private static Pattern NAME_AND_INDEX_RANGE = Pattern.compile("^([A-Z]+)\\*(\\d+):(\\d+)$");

    private Object[] splitCommand() {
        String name = "";
        boolean isMulti = false;
        int from = -1;
        int to = -1;
        String[] values;

        int indexReadSign = command.indexOf('?');
        int indexWriteSign = command.indexOf('=');

        String commandAndIndex;
        String commandValue;

        if (indexReadSign > 0) {
            commandAndIndex = command.substring(0, indexReadSign);
            commandValue = "";
        } else if (indexWriteSign > 0) {
            commandAndIndex = command.substring(0, indexWriteSign);
            commandValue = command.substring(indexWriteSign + 1);
        } else {
            commandAndIndex = command;
            commandValue = "";
        }

        Matcher m0 = NAME.matcher(commandAndIndex);
        if (m0.matches()) {
            name = m0.group(1);
            isMulti = false;
            from = -1;
            to = -1;
            values = new String[] {};
        } else {
            Matcher m1 = NAME_AND_INDEX.matcher(commandAndIndex);
            if (m1.matches()) {
                name = m1.group(1);
                isMulti = false;
                from = Integer.valueOf(m1.group(2));
                to = from;
                values = new String[] { commandValue };
            } else {
                Matcher m2 = NAME_AND_INDEX_RANGE.matcher(commandAndIndex);
                if (m2.matches()) {
                    name = m2.group(1);
                    isMulti = true;
                    from = Integer.valueOf(m2.group(2));
                    to = Integer.valueOf(m2.group(3));
                    values = commandValue.split("\t");
                } else {
                    name = "";
                    isMulti = false;
                    from = -1;
                    to = -1;
                    values = null;
                }
            }
        }

        Object[] arr = new Object[5];
        arr[0] = name;
        arr[1] = isMulti;
        arr[2] = from;
        arr[3] = to;
        arr[4] = values;

        return arr;
    }

    /**
     * Calculate CRC for Command based on original character(not encrypted)
     * and CRC array Value
     */
    private String calcCommandCRC(String cmdStr) {
        byte[] cmdBytes = cmdStr.getBytes();
        int sum = 65535;

        for (int i = 0; i < cmdBytes.length; i++) {
            sum = (sum >> 8) ^ RiscoBindingConstants.CRCArray[((sum) ^ (cmdBytes[i] & 0xff)) & 0xff];
        }

        byte b1 = (byte) (sum >> 8);
        byte b2 = (byte) (sum & 0xff);
        byte[] bts = new byte[] { b1, b2 };

        return HexUtils.bytesToHex(bts);
    }

    /*
     * Create the pseudo buffer used to encode/decode communication
     */
    private byte[] createPseudoBuffer(int panelId) {
        int bufferLength = 255;
        byte[] pseudoBuffer = new byte[bufferLength];
        int pid = panelId;

        int[] numArray = new int[] { 2, 4, 16, 32768 };
        if (pid != 0) {
            for (int index = 0; index < bufferLength; index++) {
                int n1 = 0;
                int n2 = 0;

                for (n1 = 0; n1 < 4; n1++) {
                    if ((pid & numArray[n1]) > 0) {
                        n2 ^= 1;
                    }
                }
                pid = pid << 1 | n2;
                pseudoBuffer[index] = (byte) (pid & 0xFF);
            }
        } else {
            Arrays.fill(pseudoBuffer, (byte) 0);
        }

        logger.trace("Pseudo Buffer Created for Panel Id({})", panelId);

        return pseudoBuffer;
    }

    /**
     * Encryption/Decryption mechanism
     */
    private byte[] encrypt(String fullCommand, Boolean encrypt) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int offset = 0;
        int position = 0;

        byte[] buffer = fullCommand.getBytes(); // fullCommand.getBytes(encoding);
        byte[] encryptionBuffer = createPseudoBuffer(panelId);

        for (int i = 0; i < buffer.length; i++) {
            if (encrypt) {
                buffer[i] ^= encryptionBuffer[position - offset];
            }

            switch (buffer[i]) {
                case 2:
                case 3:
                case 16:
                    outputStream.write(0x10);
            }

            outputStream.write(buffer[i]);
            position++;
        }

        byte[] encryptedChars = outputStream.toByteArray();
        return encryptedChars;
    }

    private byte[] decrypt(byte[] encrypted) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Remove DLE chars
        for (int i = 0; i < encrypted.length; i++) {
            if ((encrypted[i] == 0x10)
                    && (encrypted[i + 1] == 0x02 || encrypted[i + 1] == 0x03 || encrypted[i + 1] == 0x10)) {
                outputStream.write(encrypted[i + 1]);
                i++;
            } else {
                outputStream.write(encrypted[i]);
            }
        }
        byte[] encryptedWithoutDle = outputStream.toByteArray();
        byte[] decryptionBuffer = createPseudoBuffer(panelId);

        // Decrypt
        int offset = 0;
        int position = 0;

        outputStream.reset();
        for (int i = (isEncrypted() ? 2 : 1); i < encryptedWithoutDle.length - 1; i++) {
            if (isEncrypted()) {
                encryptedWithoutDle[i] ^= decryptionBuffer[position - offset];
            }

            outputStream.write(encryptedWithoutDle[i]);
            logger.trace("Position: {}, i: {}, chars[i]: {}", position, i, encryptedWithoutDle[i]);

            position++;
        }
        byte[] decrypted = outputStream.toByteArray();

        logger.trace("Encrypted buffer: {}", HexUtils.bytesToHex(encrypted, " "));
        logger.trace("Decrypted buffer: {}", HexUtils.bytesToHex(decrypted, "-"));

        return decrypted;
    }

    private String bytesToString(byte[] bytes) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
