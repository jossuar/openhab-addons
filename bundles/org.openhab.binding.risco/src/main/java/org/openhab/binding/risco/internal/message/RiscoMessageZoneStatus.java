package org.openhab.binding.risco.internal.message;

import org.eclipse.jdt.annotation.NonNullByDefault;

@NonNullByDefault
public class RiscoMessageZoneStatus extends RiscoMessage {
    public RiscoMessageZoneStatus(int commandId, String commandName, String modifier, String[] commandValues,
            int indexFrom, int indexTo, byte[] encryptedMessage, byte[] decryptedMessage) {
        super(commandId, commandName, modifier, commandValues, indexFrom, indexTo, encryptedMessage, decryptedMessage);
    }

    @Override
    public ThingProperty[] getProperties() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
