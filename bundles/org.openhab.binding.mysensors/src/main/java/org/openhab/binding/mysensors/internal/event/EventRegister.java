/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.mysensors.internal.event;

import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event register generic class
 *
 * @author Andrea Cioni - Initial contribution
 *
 * @param <T> the EventListener to register
 */
@NonNullByDefault
public class EventRegister<T extends EventListener> implements Register<T> {

    private final Logger logger = LoggerFactory.getLogger(EventRegister.class);

    private final List<T> registeredEventListener;

    public EventRegister() {
        registeredEventListener = new CopyOnWriteArrayList<>();
    }

    @Override
    public boolean isEventListenerRegisterd(T listener) {
        return registeredEventListener.contains(listener);
    }

    @Override
    public void addEventListener(T listener) {
        if (!isEventListenerRegisterd(listener)) {
            logger.trace("Adding listener {} to {}", listener, this);
            registeredEventListener.add(listener);
        } else {
            logger.debug("Event listener {} already registered", listener);
        }
    }

    @Override
    public void removeEventListener(T listener) {
        if (isEventListenerRegisterd(listener)) {
            logger.trace("Removing listener {} from {}", listener, this);
            registeredEventListener.remove(listener);
        } else {
            logger.debug("Listener {} not present, cannot remove it", listener);
        }
    }

    @Override
    public void clearAllListeners() {
        logger.trace("Clearing all listeners from {}", this);
        registeredEventListener.clear();
    }

    @Override
    public List<T> getEventListeners() {
        return registeredEventListener;
    }
}
