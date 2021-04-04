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
package org.openhab.binding.caddx.internal.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.caddx.internal.handler.ThingHandlerKeypad;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the automation engine action handler service for the
 * caddx bridge actions.
 *
 * @author Georgios Moutsos - Initial contribution
 */
@ThingActionsScope(name = "caddx")
@NonNullByDefault
public class CaddxKeypadActions implements ThingActions {
    private final Logger logger = LoggerFactory.getLogger(CaddxKeypadActions.class);

    private static final String HANDLER_IS_NULL = "ThingHandlerKeypad is null!";
    private static final String LINE1_IS_NULL = "The value for the 1st line is null. Action not executed.";
    private static final String LINE2_IS_NULL = "The value for the 2nd line is null. Action not executed.";
    private static final String SECONDS_IS_NULL = "The value for the seconds is null. Action not executed.";
    private static final String SECONDS_IS_INVALID = "The value for the seconds is invalid. Action not executed.";
    private @Nullable ThingHandlerKeypad handler;

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof ThingHandlerKeypad) {
            this.handler = (ThingHandlerKeypad) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return this.handler;
    }

    @RuleAction(label = "enterTerminalMode", description = "Enter terminal mode on the selected keypad")
    public void enterTerminalMode(
            @ActionInput(name = "seconds", label = "Number of seconds to stay in terminal mode", description = "Number of seconds to stay in terminal mode") @Nullable String seconds) {
        ThingHandlerKeypad thingHandler = this.handler;
        if (thingHandler == null) {
            logger.debug(HANDLER_IS_NULL);
            return;
        }
        if (seconds == null) {
            logger.debug(SECONDS_IS_NULL);
            return;
        }
        try {
            int secs = Integer.parseInt(seconds);
            if (secs < 0 || secs > 200) {
                logger.debug(SECONDS_IS_INVALID);
                return;
            }
        } catch (NumberFormatException ex) {
            logger.debug(SECONDS_IS_INVALID);
            return;
        }

        thingHandler.enterTerminalMode(seconds);
    }

    public static void enterTerminalMode(ThingActions actions, @Nullable String seconds) {
        ((CaddxKeypadActions) actions).enterTerminalMode(seconds);
    }

    @RuleAction(label = "sendKeypadTextMessage", description = "Display a message on the Keypad")
    public void sendKeypadTextMessage(
            @ActionInput(name = "line1", label = "Line 1 text", description = "The text to be displayed on the 1st line") @Nullable String line1,
            @ActionInput(name = "line2", label = "Line 2 text", description = "The text to be displayed on the 2nd line") @Nullable String line2) {
        ThingHandlerKeypad thingHandler = handler;
        if (thingHandler == null) {
            logger.debug(HANDLER_IS_NULL);
            return;
        }

        if (line1 == null) {
            logger.debug(LINE1_IS_NULL);
            return;
        }

        if (line2 == null) {
            logger.debug(LINE2_IS_NULL);
            return;
        }

        // Adjust parameters
        String paddedLine1 = line1 + "                ";
        paddedLine1 = paddedLine1.substring(0, 16);
        String paddedLine2 = line2 + "                ";
        paddedLine2 = paddedLine2.substring(0, 16);

        // Build the command
        thingHandler.sendKeypadTextMessage(paddedLine1, paddedLine2);
    }

    public static void sendKeypadTextMessage(ThingActions actions, @Nullable String displayLocation,
            @Nullable String text) {
        ((CaddxKeypadActions) actions).sendKeypadTextMessage(displayLocation, text);
    }
}
