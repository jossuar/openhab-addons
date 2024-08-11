/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.risco.internal.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.risco.internal.handler.thing.RiscoPartitionHandler;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the automation engine action handler service for the
 * risco partition actions.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@ThingActionsScope(name = "risco")
@NonNullByDefault
public class RiscoPartitionActions implements ThingActions {
    private final Logger logger = LoggerFactory.getLogger(RiscoPartitionActions.class);

    private static final String HANDLER_IS_NULL = "RiscoPartitionHandler is null!";
    private static final String PIN_IS_NULL = "Pin is null!";

    private @Nullable RiscoPartitionHandler handler;

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof RiscoPartitionHandler partitionHandler) {
            this.handler = partitionHandler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return this.handler;
    }

    @RuleAction(label = "disarm", description = "Dis-arm")
    public void disarm(@ActionInput(name = "pin", label = "pin", description = "The pin") @Nullable String pin) {
        RiscoPartitionHandler handler = this.handler;
        if (handler == null) {
            logger.debug(HANDLER_IS_NULL);
            return;
        }
        if (pin == null) {
            logger.debug(PIN_IS_NULL);
            return;
        }

        handler.disarm(pin);
    }

    public static void disarm(ThingActions actions, @Nullable String pin) {
        ((RiscoPartitionActions) actions).disarm(pin);
    }

    @RuleAction(label = "arm", description = "Arm in away mode")
    public void arm(@ActionInput(name = "pin", label = "pin", description = "The pin") @Nullable String pin) {
        RiscoPartitionHandler handler = this.handler;
        if (handler == null) {
            logger.debug(HANDLER_IS_NULL);
            return;
        }
        if (pin == null) {
            logger.debug(PIN_IS_NULL);
            return;
        }

        handler.arm(pin);
    }

    public static void armInAwayMode(ThingActions actions, @Nullable String pin) {
        ((RiscoPartitionActions) actions).arm(pin);
    }

    @RuleAction(label = "stay", description = "Arm in stay mode")
    public void stay(@ActionInput(name = "pin", label = "pin", description = "The pin") @Nullable String pin) {
        RiscoPartitionHandler handler = this.handler;
        if (handler == null) {
            logger.debug(HANDLER_IS_NULL);
            return;
        }
        if (pin == null) {
            logger.debug(PIN_IS_NULL);
            return;
        }

        handler.stay(pin);
    }

    public static void stay(ThingActions actions, @Nullable String pin) {
        ((RiscoPartitionActions) actions).stay(pin);
    }
}
