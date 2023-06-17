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
import java.util.concurrent.Executors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.risco.internal.RiscoCommunicator.RiscoPanelListener;
import org.openhab.binding.risco.internal.message.RiscoMessagePair;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * Test class for Risco communication.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
public class RiscoCommTest implements RiscoPanelListener {
    private final Logger logger = (Logger) LoggerFactory.getLogger(RiscoCommTest.class);

    @Test
    public void testCommunication() throws IOException {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.DEBUG);

        /*
         * LoggerContext logCtx = (LoggerContext) LoggerFactory.getILoggerFactory();
         *
         * PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
         * logEncoder.setContext(logCtx);
         * logEncoder.setPattern("%-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level – %msg%n");
         * logEncoder.start();
         *
         * ConsoleAppender logConsoleAppender = new ConsoleAppender();
         * logConsoleAppender.setContext(logCtx);
         * logConsoleAppender.setName("console");
         * logConsoleAppender.setEncoder(logEncoder);
         * logConsoleAppender.start();
         *
         * root.addAppender(logConsoleAppender);
         */

        logger.trace("Hello trace message");
        logger.debug("Hello debug message");
        logger.warn("Hello trace message");

        logger.trace("Root trace message");
        logger.debug("Root debug message");
        logger.warn("Root trace message");

        RiscoCommunicator comm = new RiscoCommunicator("risco", "192.168.1.120", 1000, 1, "UTF-8", "5678",
                Executors.newScheduledThreadPool(1));
        comm.addListener(this);

        // comm.send("RMT=5678");
        // comm.send("LCL");
        comm.send("ZLBL*1:16?");
        comm.send("PNLCNF");
        comm.send("SYSLBL?");
        comm.send("SSTT?");
        comm.send("UOSTT1?");
        comm.send("OSTT1?");
        comm.send("ZTYPE*1:8?");
        comm.send("ZPART&*1?");
        comm.send("ZAREA&*1?");

        int mins = 0;
        int max_mins = 100 * 6 * 60;
        while (true) {
            try {
                Thread.sleep(3 * 1000);
                mins++;
                if (mins >= max_mins) {
                    break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        comm.stop();
    }

    @Override
    public void handleRiscoMessage(RiscoMessagePair pair) {
        // logger.warn("ooooooooooooooooooooooooooooooooooooooooooooooooooo Updating the channels");
    }
}
