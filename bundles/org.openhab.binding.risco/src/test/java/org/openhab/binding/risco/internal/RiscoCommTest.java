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

import java.io.IOException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;

/**
 * Test class for Risco communication.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoCommTest {

    @Test
    public void testCommunication() throws IOException {
        RiscoCommunicator comm = new RiscoCommunicator("risco", "192.168.1.120", 1000, 1, "UTF-8");

        comm.send("RMT=5678");
        comm.send("LCL");
        comm.send("ZLBL*1:8?");
        comm.send("PNLCNF");
        comm.send("SYSLBL?");
        comm.send("SSTT?");
        comm.send("ZTYPE*1?");
        comm.send("ZPART&*1?");
        comm.send("ZAREA&*1?");

        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        comm.stop();
    }
}
