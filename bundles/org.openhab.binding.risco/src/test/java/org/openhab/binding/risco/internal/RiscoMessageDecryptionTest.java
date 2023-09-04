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
package org.openhab.binding.risco.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoMessageFactory;
import org.openhab.core.util.HexUtils;

/**
 * Test class for Risco Message.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoMessageDecryptionTest {
    private static final String MESSAGE_EXT = ".txt";
    private int counter = 1;

    // @formatter:off
    public static final List<Object> data() {
        return Arrays.asList(new Object []
            //{"",},
            { "PANEL_MESSAGES" });
    }
    // @formatter:on

    @Disabled
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageHandling(String messageFile) {
        List<byte[]> messages = readRiscoMessages(messageFile + MESSAGE_EXT);

        for (byte[] m : messages) {
            checkDecryptEncrypt(m);
        }
    }

    private static List<byte[]> readRiscoMessages(String messageFile) {
        List<byte[]> messages = null;

        try (InputStream is = MessageReaderUtil.class.getResourceAsStream(messageFile);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr)) {
            messages = br.lines().map((hexString) -> {
                return HexUtils.hexToBytes(hexString);
            }).collect(Collectors.toList());

        } catch (IOException e) {
            throw new AssertionError("IOException reading message data: ", e);
        }

        return messages;
    }

    private void checkDecryptEncrypt(byte[] bytes) {
        // Create 1st message from byte array
        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg = factory.create(1, "UTF-8", bytes);

        // Print message information
        PrintStream console = System.out;
        if (console != null) {
            console.println(counter++);
            String ss = msg.toString();
            console.println(ss);
            console.println();
            console.flush();
        }

        // Create 2nd message from 1st message parts
        RiscoMessage msg2 = factory.create(1, "UTF-8", msg.getCommandId(), msg.getFullCommand(), msg.isEncrypted());

        // Check
        assertArrayEquals(msg.getEncryptedMessage(), msg2.getEncryptedMessage());
        assertArrayEquals(msg.getDecryptedMessage(), msg2.getDecryptedMessage());
    }
}
