package org.openhab.binding.risco.internal;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.openhab.core.util.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RiscoCrypt {
    private final Logger logger = LoggerFactory.getLogger(RiscoCrypt.class);
    private final String endOfBlockString = Character.toString((char) 23);

    /**
     * Create a Crypt Object for Encoding / Decoding Risco Communication
     * This Pseudo Buffer is based on Panel Id
     * When Panel Id is 0, no encryption is applied
     * Default Panel Id is 0001
     */

    private int panelId;
    private String encoding; // readonly encoding:BufferEncoding;

    private byte[] cryptBuffer; // :Uint8Array;
    private final int[] CRCArray; // :Uint16Array;
    private boolean cryptCommands; // :boolean;

    public RiscoCrypt(int panelId, String encoding) {
        this.panelId = panelId;
        this.encoding = encoding;

        this.cryptBuffer = this.createPseudoBuffer(this.panelId);
        this.cryptCommands = false;
        this.CRCArray = RiscoBindingConstants.CRCArray;
    }

    /**
     * Decode received message and extract Command id, command and CRC Value
     *
     * @param {string} message
     * @return {number} Command Id
     *         {string} Command itself
     *         {boolean} isValidCRC
     */
    public Object[] decodeMessage(byte[] message) {
        this.cryptCommands = RiscoCrypt.isEncrypted(message);
        byte[] decryptedMsgBytes = this.decryptChars(message);
        String decryptedMessage = this.byteToString(decryptedMsgBytes);

        Integer cmdId;
        String commandStr;
        String crcValue;
        if (decryptedMessage.startsWith("N") || decryptedMessage.startsWith("B")) {
            cmdId = null;
            commandStr = decryptedMessage.substring(0, decryptedMessage.indexOf(endOfBlockString));
            crcValue = decryptedMessage.substring(decryptedMessage.indexOf(endOfBlockString) + 1);
        } else {
            cmdId = Integer.parseInt(decryptedMessage.substring(0, 2), 10);
            commandStr = decryptedMessage.substring(2, decryptedMessage.indexOf(endOfBlockString));
            crcValue = decryptedMessage.substring(decryptedMessage.indexOf(endOfBlockString) + 1);
        }

        return new Object[] { cmdId, commandStr, cryptCommands, this.isValidCRC(cmdId, decryptedMessage, crcValue) };
    }

    /**
     * Encode the Message with PseudoBuffer
     * Each Char is XOred with same index char in PseudoBuffer
     * Some char are added (start of frame = 2, end of frame = 3 and encryption indicator = 17)
     *
     * command example :
     * 01RMT=5678 ABCD
     * Where :
     * 01 => command number (from 01 to 49)
     * RMT= => command itself (ReMoTe)
     * 5678 => Default passcode for Remote
     * ABCD => CRC Value
     *
     */
    public byte[] getCommandBuffer(String command, int cmdId, boolean forceCrypt) {
        // Add Cmd_Id to command and Separator character between Cmd and CRC value
        String FullCmd = String.format("%02d", cmdId) + command + Character.toString(23);
        String CRCValue = this.getCommandCRC(FullCmd);

        // Encrypt command
        this.cryptCommands = forceCrypt;
        byte[] e = encryptChars(FullCmd + CRCValue);

        ByteArrayOutputStream encrypedOutputStream = new ByteArrayOutputStream();
        // byte = 2 => start of command
        encrypedOutputStream.write(2);
        if (forceCrypt) {
            // byte = 17 => encryption indicator
            encrypedOutputStream.write(17);
        }
        encrypedOutputStream.write(e, 0, e.length);
        // byte = 3 => end of command
        encrypedOutputStream.write(3);

        byte[] encrypted = encrypedOutputStream.toByteArray();
        return encrypted;
    }

    public void updatePanelId(int panelId) {
        this.panelId = panelId;
        this.cryptBuffer = this.createPseudoBuffer(this.panelId);
    }

    /*
     * Create the pseudo buffer used to encode/decode communication
     */
    private byte[] createPseudoBuffer(int panelId) {
        int BufferLength = 255;
        byte[] PseudoBuffer = new byte[BufferLength];
        int pid = panelId;

        int[] numArray = new int[] { 2, 4, 16, 32768 };
        if (pid != 0) {
            for (int index = 0; index < BufferLength; index++) {
                int n1 = 0;
                int n2 = 0;

                for (n1 = 0; n1 < 4; n1++) {
                    if ((pid & numArray[n1]) > 0) {
                        n2 ^= 1;
                    }
                }
                pid = pid << 1 | n2;
                PseudoBuffer[index] = (byte) (pid & BufferLength);
            }
        } else {
            Arrays.fill(PseudoBuffer, (byte) 0);
        }
        logger.debug("Pseudo Buffer Created for Panel Id({})", this.panelId);

        return PseudoBuffer;
    }

    /**
     * Convert String to byte array
     */
    private byte[] stringToByte(String command) {
        return command.getBytes();
    }

    /**
     * Convert String to byte array
     *
     * @throws UnsupportedEncodingException
     */
    private String byteToString(byte[] bytes) {
        try {
            return new String(bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /*
     * Verify if Received Data CRC is OK
     *
     * @param {string} decryptedMessage
     *
     * @param {string} receivedCrc
     *
     * @return {boolean}
     */
    private boolean isValidCRC(Integer CmdId, String decryptedMessage, String receivedCrc) {
        if (receivedCrc.length() != 4) {
            logger.debug("Command[{}] Incorrect crc : expecting 4 chars length, got {}", CmdId, receivedCrc.length());
            return false;
        }

        for (int i = 0; i < 4; i++) {
            if (receivedCrc.charAt(i) > 127) {
                logger.debug("Command[{}] Incorrect crc : expecting ascii only chars", CmdId);
                return false;
            }
        }

        String strNoCRC = decryptedMessage.substring(0, decryptedMessage.indexOf(endOfBlockString) + 1);

        String computedCrc = this.getCommandCRC(strNoCRC);
        boolean crcOK = receivedCrc.equals(computedCrc);
        logger.debug("Command[{}] crcOK:{}, Computed CRC: {}, Message CRC: {}", CmdId, crcOK, computedCrc, receivedCrc);

        return crcOK;
    }

    /**
     * Calculate CRC for Command based on original character(not encrypted)
     * and CRC array Value
     */
    private String getCommandCRC(String cmdStr) {
        byte[] CmdBytes = this.stringToByte(cmdStr);
        int sum = 65535;

        for (int i = 0; i < CmdBytes.length; i++) {
            sum = (sum >> 8) ^ CRCArray[((sum) ^ (CmdBytes[i] & 0xff)) & 0xff];
        }

        byte b1 = (byte) (sum >> 8);
        byte b2 = (byte) (sum & 0xff);
        byte[] bts = new byte[] { b1, b2 };

        return HexUtils.bytesToHex(bts, "");
    }

    /**
     * Encryption/Decryption mechanism
     */
    private byte[] encryptChars(String charsCmd) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int offset = 0;
        int position = 0;

        byte[] chars = stringToByte(charsCmd); // Buffer.from(charsCmd, this.encoding);
        for (int i = 0; i < chars.length; i++) {
            if (this.cryptCommands) {
                chars[i] ^= this.cryptBuffer[position - offset];
            }

            switch (chars[i]) {
                case 2:
                case 3:
                case 16:
                    outputStream.write(16);
            }

            outputStream.write(chars[i]);
            position++;
        }

        byte[] encryptedChars = outputStream.toByteArray();
        return encryptedChars;
    }

    /**
     * Decryption mechanism
     */
    private byte[] decryptChars(byte[] charsCmd) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (int i = 0; i < charsCmd.length; i++) {
            if ((charsCmd[i] == 16) && (charsCmd[i + 1] == 2 || charsCmd[i + 1] == 3 || charsCmd[i + 1] == 16)) {
                outputStream.write(charsCmd[i + 1]);
                i++;
            } else {
                outputStream.write(charsCmd[i]);
            }
        }
        byte[] escapedDleChars = outputStream.toByteArray();

        boolean decrypt = RiscoCrypt.isEncrypted(charsCmd);
        int offset = 0;
        int position = 0;

        outputStream.reset();
        for (int i = (decrypt ? 2 : 1); i < escapedDleChars.length - 1; i++) {
            if (decrypt) {
                escapedDleChars[i] ^= this.cryptBuffer[position - offset];
            }

            outputStream.write(escapedDleChars[i]);
            logger.debug("Position: {}, i: {}, chars[i]: {}", position, i, escapedDleChars[i]);
            position++;
        }
        byte[] decryptedChars = outputStream.toByteArray();
        // logger.debug("Output: {}", this.byteToString(decryptedChars));

        return decryptedChars;
    }

    private static boolean isEncrypted(byte[] data) {
        return data[1] == 17;
    }
}
