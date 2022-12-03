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
package org.openhab.binding.mysensors.action;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.mysensors.handler.MySensorsThingHandler;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MySensorsActions} defines thing actions for MySensorsThingHandler.
 *
 * @author Andriy Yemets - Initial contribution
 */
@ThingActionsScope(name = "mysensors")
@NonNullByDefault
public class MySensorsActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private @Nullable MySensorsThingHandler handler;

    public MySensorsActions() {
        logger.debug("MySensors actions service instantiated");
    }

    @Override
    public void setThingHandler(ThingHandler handler) {
        if (handler instanceof MySensorsThingHandler) {
            this.handler = (MySensorsThingHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return handler;
    }

    /**
     * Reboot thing action
     */
    @RuleAction(label = "reboot the node", description = "Reboot the MySensors Node")
    public void reboot() {
        MySensorsThingHandler nodeHandler = this.handler;
        if (nodeHandler != null) {
            nodeHandler.handleNodeReboot();
        } else {
            logger.warn("MySensors Actions service ThingHandler is null! ");
        }
    }

    /**
     * Presentation thing action
     */
    @RuleAction(label = "presentation the node", description = "Presentation the MySensors Node")
    public void presentation() {
        MySensorsThingHandler nodeHandler = this.handler;
        if (nodeHandler != null) {
            nodeHandler.handleNodePresentation();
        } else {
            logger.warn("MySensors Actions service ThingHandler is null! ");
        }
    }

    // Static method for Rules DSL backward compatibility
    public static void reboot(ThingActions actions) {
        ((MySensorsActions) actions).reboot();
    }

    // Static method for Rules DSL backward compatibility
    public static void presentation(ThingActions actions) {
        ((MySensorsActions) actions).presentation();
    }

}
