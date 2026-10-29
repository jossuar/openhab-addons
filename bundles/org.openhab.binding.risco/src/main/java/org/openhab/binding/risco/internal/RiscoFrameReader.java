/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link RiscoFrameReader} reads message frames sent by the panel.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
class RiscoFrameReader {
    static final int STX = 0x02;
    static final int ETX = 0x03;
    static final int DLE = 0x10;

    private final Logger logger = LoggerFactory.getLogger(RiscoFrameReader.class);

    private final InputStream in;
    private final int maxSize;

    RiscoFrameReader(InputStream in, int maxSize) {
        this.in = in;
        this.maxSize = maxSize;
    }

    public byte[] readFrame() throws IOException {
        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        skipToStart();
        frame.write(STX);

        while (true) {
            int b = readByte();

            if (b == DLE) {
                frame.write(DLE);
                frame.write(readByte());
            } else if (b == STX) {
                // An unescaped STX can only start a frame, so the current one was cut short
                logger.debug("Incomplete frame discarded: a new frame started before its end.");
                frame.reset();
                frame.write(STX);
            } else {
                frame.write(b);
                if (b == ETX) {
                    return frame.toByteArray();
                }
            }

            if (frame.size() > maxSize) {
                logger.debug("Frame longer than {} bytes discarded.", maxSize);
                frame.reset();
                skipToStart();
                frame.write(STX);
            }
        }
    }

    private void skipToStart() throws IOException {
        while (true) {
            int b = readByte();
            if (b == STX) {
                return;
            }
            if (b == DLE) {
                readByte();
            }
        }
    }

    private int readByte() throws IOException {
        int b = in.read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }
}
