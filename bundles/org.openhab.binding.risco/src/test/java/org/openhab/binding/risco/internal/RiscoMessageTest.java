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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.binding.risco.internal.protocol.RiscoMessage;
import org.openhab.binding.risco.internal.protocol.RiscoMessageFactory;
import org.openhab.binding.risco.internal.protocol.message.general.Unknown;
import org.openhab.binding.risco.internal.protocol.message.general.ZoneLabel;
import org.openhab.binding.risco.internal.protocol.message.status.ZoneStatus;

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
            //{"",},
            { "CUSTLST_write", "CUSTLST", -1, -1, Unknown.class },
            { "CLOCK_write", "CLOCK", -1, -1, Unknown.class },
            { "DTYPZ_1-8_read", "DTYPZ", 1, 8, Unknown.class },
            { "DTYPZ_1-8_write", "DTYPZ", 1, 8, Unknown.class },
            { "N13", "N13", -1, -1, Unknown.class },
            { "ZLBL_1-8_write", "ZLBL", 1, 8, ZoneLabel.class },
            { "ZSTT_1-8_read", "ZSTT", 1, 8, ZoneStatus.class },
            { "ZSTT_19_read", "ZSTT", 19, 19, ZoneStatus.class },
            { "ZSTT_19_write_on", "ZSTT", 19, 19, ZoneStatus.class },
            { "ZSTT_19_write_off", "ZSTT", 19, 19, ZoneStatus.class },
            { "ZTYPE_17-24_write", "ZTYPE", 17, 24, Unknown.class },
        });
    }
    // @formatter:on

    // @Disabled
    @ParameterizedTest
    @MethodSource("data")
    public void testMessageHandling(String messageName, String commandName, int indexFrom, int indexTo,
            Class<RiscoMessage> type) {
        byte[] bytes = MessageReaderUtil.readRiscoMessage(messageName);

        // Create msg1 from byte array
        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg1 = factory.create(1, "UTF-8", bytes);

        assertEquals(msg1.getClass(), type);
        assertEquals(commandName, msg1.getCommandName());
        assertEquals(indexFrom, msg1.getIndexFrom());
        assertEquals(indexTo, msg1.getIndexTo());

        // Create msg2 from msg1 parts
        RiscoMessage msg2 = factory.create(1, "UTF-8", msg1.getCommandId(), msg1.getFullCommand(), msg1.isEncrypted());

        // Create msg3 from msg2 encrypted byte array
        RiscoMessage msg3 = factory.create(1, "UTF-8", msg2.getEncryptedMessage());

        /*
         * PrintStream console = System.out;
         * if (console != null) {
         * console.println(msg1.toString());
         * console.println(msg2.toString());
         * console.println(msg3.toString());
         * console.println();
         * console.flush();
         * }
         */

        // Check msg1 and msg3 decrypted bytes
        assertArrayEquals(msg1.getDecryptedMessage(), msg3.getDecryptedMessage());
    }
}
