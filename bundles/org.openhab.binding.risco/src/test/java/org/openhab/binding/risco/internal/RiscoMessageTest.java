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
package org.openhab.binding.risco.internal;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test class for Risco Message.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoMessageTest {

    // @formatter:off
    public static final List<Object[]> data() {
        return Arrays.asList(new Object [][]{
            {"unencrypted_message",},
            {"checksum_message",},
            {"checksum2_message",},
            {"correct_message",},
        });
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource("data")
    public void testDecryptEncrypt(String messageName) {
        byte[] bytes = MessageReaderUtil.readRiscoMessage(messageName);

        // Decrypt
        RiscoMessage msg = new RiscoMessage(1, "UTF-8", bytes);

        // Get parts
        Integer cmdId = msg.getCommandId();
        String commandStr = msg.getCommand();
        Boolean encrypted = msg.isEncrypted();

        if (cmdId != null) {
            RiscoMessage msg2 = new RiscoMessage(1, "UTF-8", cmdId, commandStr, encrypted);
            assertArrayEquals(msg.getEncryptedMessage(), msg2.getEncryptedMessage());
            assertArrayEquals(msg.getDecryptedMessage(), msg2.getDecryptedMessage());
        }
    }
}
