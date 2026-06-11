package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.triggers.WiredTriggerUserClicksUser;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wired.WiredClickUserResponseMessageComposer;

public class WiredClickUserMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int roomUnitId = this.packet.readInt();

        Habbo clicker = this.client.getHabbo();
        if (clicker == null || clicker.getHabboInfo() == null || clicker.getRoomUnit() == null) {
            return;
        }

        Room room = clicker.getHabboInfo().getCurrentRoom();
        if (room == null) {
            return;
        }

        Habbo target = room.getHabboByRoomUnitId(roomUnitId);
        if (target == null || target.getRoomUnit() == null || target.getHabboInfo().getCurrentRoom() != room) {
            return;
        }

        boolean openMenu = true;
        for (InteractionWiredTrigger trigger : room.getRoomSpecialTypes().getTriggers(WiredTriggerType.CLICK_USER)) {
            if (trigger instanceof WiredTriggerUserClicksUser
                    && ((WiredTriggerUserClicksUser) trigger).blocksMenuOpen()) {
                openMenu = false;
                break;
            }
        }

        WiredManager.triggerUserClicksUser(room, clicker.getRoomUnit(), target.getRoomUnit());

        // May sends the clicked unit id and whether the normal avatar menu may open.
        // Full client-side menu suppression can build on this response without changing
        // the trigger activation path again.
        this.client.sendResponse(new WiredClickUserResponseMessageComposer(roomUnitId, openMenu));
    }
}
