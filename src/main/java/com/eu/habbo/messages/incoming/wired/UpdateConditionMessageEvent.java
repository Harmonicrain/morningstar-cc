package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
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

public class UpdateConditionMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {
            if (room.hasRights(this.client.getHabbo()) || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId() || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER) || this.client.getHabbo().hasPermission(Permission.ACC_MOVEROTATE)) {
                InteractionWiredCondition condition = room.getRoomSpecialTypes().getCondition(itemId);

                if (condition != null) {
                    // Wired 2.0: deterministic dispatch (no reflection). saveData is the
                    // typed abstract on InteractionWiredCondition; settings come from the
                    // 2.0 reader bridged to the legacy DTO.
                    WiredSettings settings = InteractionWired.readSettingsNew(this.packet, WiredCategoryType.CONDITION).toLegacy();

                    if (condition.saveData(settings)) {
                        this.client.sendResponse(new WiredSavedMessageComposer());
                        condition.needsUpdate(true);
                        Emulator.getThreading().run(condition);
                        WiredManager.invalidateRoom(room);
                    } else {
                        this.client.sendResponse(new WiredValidationErrorMessageComposer("There was an error while saving that condition"));
                    }
                }
            }
        }
    }
}
