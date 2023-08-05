package org.openhab.binding.risco.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class RiscoMessageUnknown extends RiscoMessage {

    public RiscoMessageUnknown(int commandId, String commandName, String modifier, String[] commandValues,
            int indexFrom, int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public ThingProperty[] getProperties() {
        // Return an empty property array
        return new ThingProperty[0];
    }
}
