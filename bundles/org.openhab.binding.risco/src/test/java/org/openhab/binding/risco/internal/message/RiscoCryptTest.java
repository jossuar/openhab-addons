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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.binding.risco.internal.MessageReaderUtil;
import org.openhab.binding.risco.internal.RiscoCrypt;

/**
 * Test class for Risco encryption
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoCryptTest {

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
        RiscoCrypt rc = new RiscoCrypt(1, "UTF-8");
        byte[] bytes = MessageReaderUtil.readRiscoMessage(messageName);

        Object[] obj = rc.decodeMessage(bytes);
        if (obj.length == 4) {
            Integer cmdId = (Integer) obj[0];
            String commandStr = (String) obj[1];
            Boolean encrypted = (Boolean) obj[2];
            // Boolean crcOk = (Boolean) obj[3];

            byte[] encodedBuffer = rc.getCommandBuffer(commandStr, cmdId, encrypted);

            assertArrayEquals(bytes, encodedBuffer);
        }
    }
}
