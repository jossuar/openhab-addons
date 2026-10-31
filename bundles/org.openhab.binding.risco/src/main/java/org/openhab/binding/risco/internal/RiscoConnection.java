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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A single TCP connection to the panel. It writes frames and reads them on its own thread.
 *
 * A connection is used once: when it closes, a new one is opened. When it fails, the listener is told once;
 * closing it deliberately does not notify the listener.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@NonNullByDefault
class RiscoConnection implements Closeable {
    interface Listener {
        void frameReceived(byte[] frame);

        void connectionFailed(IOException cause);
    }

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int MAX_FRAME_SIZE = 4096;

    private final Logger logger = LoggerFactory.getLogger(RiscoConnection.class);

    private final Socket socket;
    private final OutputStream output;
    private final RiscoFrameReader reader;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile @Nullable Listener listener;

    private RiscoConnection(Socket socket) throws IOException {
        this.socket = socket;
        this.output = new BufferedOutputStream(socket.getOutputStream());
        this.reader = new RiscoFrameReader(new BufferedInputStream(socket.getInputStream()), MAX_FRAME_SIZE);
    }

    /**
     * Opens a connection. Frames are read only after {@link #startReading} is called.
     */
    static RiscoConnection open(String hostname, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(hostname, port), CONNECT_TIMEOUT_MS);
            return new RiscoConnection(socket);
        } catch (IOException | RuntimeException e) {
            closeQuietly(socket);
            throw e;
        }
    }

    void startReading(String threadName, Listener listener) {
        this.listener = listener;
        Thread thread = new Thread(() -> readFrames(listener), threadName);
        thread.setDaemon(true);
        thread.start();
    }

    synchronized void write(byte[] frame) throws IOException {
        try {
            output.write(frame);
            output.flush();
        } catch (IOException e) {
            fail(e);
            throw e;
        }
    }

    boolean isOpen() {
        return !closed.get();
    }

    @Override
    public void close() {
        shutDown();
    }

    private void readFrames(Listener listener) {
        try {
            while (isOpen()) {
                byte[] frame = reader.readFrame();
                try {
                    listener.frameReceived(frame);
                } catch (RuntimeException e) {
                    logger.debug("Error processing a frame from the panel: {}", e.getMessage(), e);
                }
            }
        } catch (EOFException e) {
            fail(new EOFException("The panel closed the connection"));
        } catch (IOException e) {
            fail(e);
        }
    }

    private void fail(IOException cause) {
        Listener listener = this.listener;
        if (shutDown() && listener != null) {
            listener.connectionFailed(cause);
        }
    }

    /**
     * Closes the socket, which also ends the reading thread blocked in a read.
     *
     * @return true if this call closed the connection, false if it was already closed
     */
    private boolean shutDown() {
        if (!closed.compareAndSet(false, true)) {
            return false;
        }
        closeQuietly(socket);
        return true;
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // Nothing else can be done
        }
    }
}
