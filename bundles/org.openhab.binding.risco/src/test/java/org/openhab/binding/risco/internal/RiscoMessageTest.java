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

import static org.junit.jupiter.api.Assertions.*;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openhab.binding.risco.internal.message.RiscoMessage;
import org.openhab.binding.risco.internal.message.RiscoMessageFactory;
import org.openhab.binding.risco.internal.message.RiscoMessageType;

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
            { "CLOCK_write", "CLOCK", -1, -1, RiscoMessageType.CLOCK },
            { "CUSTLST_write", "CUSTLST", -1, -1, RiscoMessageType.UNKNOWN },
            { "DTYPZ_1-8_read", "DTYPZ", 1, 8, RiscoMessageType.COMMAND_DTYPZ },
            { "DTYPZ_1-8_write", "DTYPZ", 1, 8, RiscoMessageType.COMMAND_DTYPZ },
            { "N13", "N", 13, 13, RiscoMessageType.UNKNOWN },
            { "ZLBL_1-8_write", "ZLBL", 1, 8, RiscoMessageType.UNKNOWN },
            { "ZSTT_1-8_read", "ZSTT", 1, 8, RiscoMessageType.STATUS_ZONE },
            { "ZSTT_19_read", "ZSTT", 19, 19, RiscoMessageType.STATUS_ZONE },
            { "ZSTT_19_write_on", "ZSTT", 19, 19, RiscoMessageType.STATUS_ZONE },
            { "ZSTT_19_write_off", "ZSTT", 19, 19, RiscoMessageType.STATUS_ZONE },
            { "ZTYPE_17-24_write", "ZTYPE", 17, 24, RiscoMessageType.UNKNOWN },
        });
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource("data")
    public void testMessageHandling(String messageName, String commandName, int indexFrom, int indexTo,
            RiscoMessageType messageType) {
        byte[] bytes = MessageReaderUtil.readRiscoMessage(messageName);

        // Decrypt
        RiscoMessageFactory factory = new RiscoMessageFactory();
        RiscoMessage msg = factory.create(1, "UTF-8", bytes);

        // Get parts
        Integer cmdId = msg.getCommandId();
        String commandStr = msg.getFullCommand();
        Boolean encrypted = msg.isEncrypted();

        assertEquals(commandName, msg.getCommandName());
        assertEquals(indexFrom, msg.getIndexFrom());
        assertEquals(indexTo, msg.getIndexTo());
        assertEquals(messageType, msg.getMessageType());

        PrintStream console = System.out;
        if (console != null) {
            console.println(msg);
            console.println("commandName: " + msg.getCommandName());
            console.println("hasIndex: " + (msg.hasIndex() ? "true" : "false") + ", hasMultipleIndexes: "
                    + (msg.hasMultipleIndexes() ? "true" : "false") + ", indexFrom: " + msg.getIndexFrom()
                    + ", indexTo: " + msg.getIndexTo());
            console.println("thingIds: " + Arrays.toString(msg.getThingIds()));
            console.println("properties: " + Arrays.toString(msg.getProperties()));
            console.println();
        }

        RiscoMessage msg2 = factory.create(1, "UTF-8", cmdId, msg.getFullCommand(), encrypted);
        assertArrayEquals(msg.getEncryptedMessage(), msg2.getEncryptedMessage());
        assertArrayEquals(msg.getDecryptedMessage(), msg2.getDecryptedMessage());
    }
}
