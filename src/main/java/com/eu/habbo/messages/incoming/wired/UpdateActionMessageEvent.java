package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredCategoryType;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.WiredValidationErrorMessageComposer;
import com.eu.habbo.messages.outgoing.wired.WiredSavedMessageComposer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class UpdateActionMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {
            if (room.hasRights(this.client.getHabbo()) || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER) || this.client.getHabbo().hasPermission(Permission.ACC_MOVEROTATE)) {
                InteractionWiredEffect effect = room.getRoomSpecialTypes().getEffect(itemId);

                try {
                    if (effect == null)
                        throw new WiredSaveException(String.format("Wired effect with item id %s not found in room", itemId));

                    // Wired 2.0: deterministic dispatch (no reflection). saveData is the
                    // typed abstract on InteractionWiredEffect (takes GameClient, may throw
                    // WiredSaveException); settings come from the 2.0 reader bridged to legacy.
                    WiredSettings settings = InteractionWired.readSettingsNew(this.packet, WiredCategoryType.EFFECT).toLegacy();
                    if (effect.saveData(settings, this.client)) {
                        this.client.sendResponse(new WiredSavedMessageComposer());
                        effect.needsUpdate(true);
                        Emulator.getThreading().run(effect);
                        WiredManager.invalidateRoom(room);
                    }
                }
                catch (WiredSaveException e) {
                    this.client.sendResponse(new WiredValidationErrorMessageComposer(e.getMessage()));
                }
            }
        }
    }
}
