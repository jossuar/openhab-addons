/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.risco.internal.protocol;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.risco.internal.RiscoBindingConstants;
import org.openhab.binding.risco.internal.protocol.message.KeypadAllocation;
import org.openhab.binding.risco.internal.protocol.message.OutputAllocation;
import org.openhab.binding.risco.internal.protocol.message.OutputStatus;
import org.openhab.binding.risco.internal.protocol.message.PanelConfiguration;
import org.openhab.binding.risco.internal.protocol.message.PartitionAllocation;
import org.openhab.binding.risco.internal.protocol.message.PartitionStatus;
import org.openhab.binding.risco.internal.protocol.message.SystemStatus;
import org.openhab.binding.risco.internal.protocol.message.Unknown;
import org.openhab.binding.risco.internal.protocol.message.WirelessModuleAllocation;
import org.openhab.binding.risco.internal.protocol.message.ZoneAllocation;
import org.openhab.binding.risco.internal.protocol.message.ZoneExpanderAllocation;
import org.openhab.binding.risco.internal.protocol.message.ZoneLabel;
import org.openhab.binding.risco.internal.protocol.message.ZoneStatus;
import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoMessageFactory} is responsible for creation of the messages
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoMessageFactory {
    private final Logger logger = LoggerFactory.getLogger(RiscoMessageFactory.class);
    private static final String ETB = Character.toString((char) 23);

    // Create a RiscoMessage from the received data
    public RiscoMessage create(int panelId, String encoding, byte[] encryptedMessage) {
        byte[] decryptedMessage = decrypt(panelId, encryptedMessage);
        String stringMessage = bytesToString(decryptedMessage, encoding);
        int commandId;
        String wholeMessage;
        String crcValue;

        if (stringMessage.startsWith("N") || stringMessage.startsWith("B")) {
            commandId = -1;
            wholeMessage = stringMessage.substring(0, stringMessage.indexOf(ETB));
            crcValue = stringMessage.substring(stringMessage.indexOf(ETB) + 1);
        } else {
            commandId = Integer.parseInt(stringMessage.substring(0, 2), 10);
            wholeMessage = stringMessage.substring(2, stringMessage.indexOf(ETB));
            crcValue = stringMessage.substring(stringMessage.indexOf(ETB) + 1);
        }

        // Computed information
        Object[] objs = splitCommand(wholeMessage);
        String commandName = (String) objs[0];
        int indexFrom = (int) objs[1];
        int indexTo = (int) objs[2];
        String sign = (String) objs[3];
        String[] commandValues = (String[]) objs[4];

        RiscoMessage msg = createMessage(commandId, commandName, sign, commandValues, indexFrom, indexTo,
                encryptedMessage, decryptedMessage);

        // Check CRC
        if (!isValidCRC(crcValue, String.format("%02d", commandId) + wholeMessage + ETB)) {
            throw new IllegalArgumentException("CRC value is not correct.");
        }

        return msg;
    }

    // Create a RiscoMessage for the data to be sent
    public RiscoMessage create(int panelId, String encoding, Integer commandId, String command, Boolean encrypt) {
        // Add Cmd_Id to command and Separator character between Cmd and CRC value
        String cmd = String.format("%02d", commandId) + command + ETB;
        String crcValue = this.calcCommandCRC(cmd);

        // Encrypt command string
        byte[] e = encrypt(panelId, cmd + crcValue, encrypt);

        // Build full encrypted byte[] message
        ByteArrayOutputStream encrypedOutputStream = new ByteArrayOutputStream();
        encrypedOutputStream.write(2);
        if (encrypt) {
            encrypedOutputStream.write(17);
        }
        encrypedOutputStream.write(e, 0, e.length);
        encrypedOutputStream.write(3);

        // Computed information
        byte[] encryptedMessage = encrypedOutputStream.toByteArray();
        byte[] decryptedMessage = decrypt(panelId, encryptedMessage);
        String stringMessage = bytesToString(decryptedMessage, encoding);
        String wholeMessage;

        if (stringMessage.startsWith("N") || stringMessage.startsWith("B")) {
            wholeMessage = stringMessage.substring(0, stringMessage.indexOf(ETB));
        } else {
            wholeMessage = stringMessage.substring(2, stringMessage.indexOf(ETB));
        }

        Object[] objs = splitCommand(wholeMessage);
        String commandName = (String) objs[0];
        int indexFrom = (int) objs[1];
        int indexTo = (int) objs[2];
        String sign = (String) objs[3];
        String[] commandValues = (String[]) objs[4];

        return createMessage(commandId, commandName, sign, commandValues, indexFrom, indexTo, encryptedMessage,
                decryptedMessage);
    }

    // Create the concrete type
    private RiscoMessage createMessage(int commandId, String commandName, String modifier, String[] commandValues,
            int indexFrom, int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        switch (commandName) {
            case KeypadAllocation.COMMAND1:
            case KeypadAllocation.COMMAND2:
                return new KeypadAllocation(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case OutputAllocation.COMMAND:
                return new OutputAllocation(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case OutputStatus.COMMAND:
                return new OutputStatus(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case PanelConfiguration.COMMAND:
                return new PanelConfiguration(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case PartitionAllocation.COMMAND:
                return new PartitionAllocation(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case PartitionStatus.COMMAND:
                return new PartitionStatus(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case SystemStatus.COMMAND:
                return new SystemStatus(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case WirelessModuleAllocation.COMMAND:
                return new WirelessModuleAllocation(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case ZoneAllocation.COMMAND1:
            case ZoneAllocation.COMMAND2:
            case ZoneAllocation.COMMAND3:
                return new ZoneAllocation(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case ZoneExpanderAllocation.COMMAND:
                return new ZoneExpanderAllocation(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case ZoneLabel.COMMAND:
                return new ZoneLabel(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            case ZoneStatus.COMMAND:
                return new ZoneStatus(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);

            default:
                return new Unknown(commandId, commandName, modifier, commandValues, indexFrom, indexTo,
                        encryptedMessage, decryptedMessage);
        }
    }

    // private static Pattern NAME = Pattern.compile("^(N\\d+|[A-Z&]+)$");
    private static final Pattern NAME_AND_INDEX = Pattern.compile("^(N\\d+|[A-Z0-9&]+?)([0-9]*)$");
    private static final Pattern NAME_AND_INDEX_RANGE = Pattern.compile("^([A-Z0-9&]+)\\*(\\d+):(\\d+)$");

    private Object[] splitCommand(String wholeMessage) {
        String name = "";
        String sign = "";
        int from = -1;
        int to = -1;
        String[] values;

        int indexReadSign = wholeMessage.indexOf('?');
        int indexWriteSign = wholeMessage.indexOf('=');

        String commandAndIndex;
        String commandValue;

        if (indexReadSign > 0) {
            commandAndIndex = wholeMessage.substring(0, indexReadSign);
            sign = "?";
            commandValue = "";
        } else if (indexWriteSign > 0) {
            commandAndIndex = wholeMessage.substring(0, indexWriteSign);
            sign = "=";
            commandValue = wholeMessage.substring(indexWriteSign + 1);
        } else {
            commandAndIndex = wholeMessage;
            sign = "";
            commandValue = "";
        }

        Matcher m1 = NAME_AND_INDEX.matcher(commandAndIndex);
        if (m1.matches()) {
            name = m1.group(1);

            String number = m1.group(2);
            if ("".equals(number)) {
                from = -1;
                to = -1;
            } else {
                from = Integer.valueOf(m1.group(2));
                to = from;
            }
            if (indexWriteSign > 0) {
                values = new String[] { commandValue };
            } else {
                values = new String[] {};
            }
        } else {
            Matcher m2 = NAME_AND_INDEX_RANGE.matcher(commandAndIndex);
            if (m2.matches()) {
                name = m2.group(1);
                from = Integer.valueOf(m2.group(2));
                to = Integer.valueOf(m2.group(3));
                if (indexWriteSign > 0) {
                    values = commandValue.split("\t", -1);
                } else {
                    values = new String[] {};
                }
            } else {
                name = "";
                from = -1;
                to = -1;
                values = new String[] {};
            }
        }

        Object[] arr = new Object[5];
        arr[0] = name;
        arr[1] = from;
        arr[2] = to;
        arr[3] = sign;
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
            sum = (sum >> 8) ^ RiscoBindingConstants.CRC_ARRAY[((sum) ^ (cmdBytes[i] & 0xff)) & 0xff];
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
    private byte[] encrypt(int panelId, String fullCommand, Boolean encrypt) {
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

    private byte[] decrypt(int panelId, byte[] encryptedMessage) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Remove DLE chars
        for (int i = 0; i < encryptedMessage.length; i++) {
            if ((encryptedMessage[i] == 0x10) && (encryptedMessage[i + 1] == 0x02 || encryptedMessage[i + 1] == 0x03
                    || encryptedMessage[i + 1] == 0x10) && (i != encryptedMessage.length - 2)) {
                outputStream.write(encryptedMessage[i + 1]);
                i++;
            } else {
                outputStream.write(encryptedMessage[i]);
            }
        }
        byte[] encryptedWithoutDle = outputStream.toByteArray();
        byte[] decryptionBuffer = createPseudoBuffer(panelId);

        // Decrypt
        int offset = 0;
        int position = 0;

        outputStream.reset();
        for (int i = (isEncrypted(encryptedMessage) ? 2 : 1); i < encryptedWithoutDle.length - 1; i++) {
            if (isEncrypted(encryptedMessage)) {
                encryptedWithoutDle[i] ^= decryptionBuffer[position - offset];
            }

            outputStream.write(encryptedWithoutDle[i]);
            // logger.trace("Position: {}, i: {}, chars[i]: {}", position, i, encryptedWithoutDle[i]);

            position++;
        }
        byte[] decryptedMessage = outputStream.toByteArray();

        logger.trace("Encrypted buffer: {}", HexUtils.bytesToHex(encryptedMessage, " "));
        logger.trace("Decrypted buffer: {}", HexUtils.bytesToHex(decryptedMessage, "-"));

        return decryptedMessage;
    }

    private String bytesToString(byte[] bytes, String encoding) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private boolean isEncrypted(byte[] encryptedMessage) {
        return encryptedMessage.length > 1 && encryptedMessage[1] == 17;
    }

    public boolean isValidCRC(String crcValue, String wholeMessage) {
        /*
         * if (crcValue.length() != 4) {
         * return false;
         * }
         *
         * for (int i = 0; i < 4; i++) {
         * if (crcValue.charAt(i) > 127) {
         * return false;
         * }
         * }
         */
        String computedCrc = calcCommandCRC(wholeMessage);
        boolean crcOK = crcValue.equals(computedCrc);

        logger.trace("Command[{}] crcOK:{}, Computed CRC: {}, Message CRC: {}", wholeMessage, crcOK, computedCrc,
                crcValue);

        return crcOK;
    }
}
