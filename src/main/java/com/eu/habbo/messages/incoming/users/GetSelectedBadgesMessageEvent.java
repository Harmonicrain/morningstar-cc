package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.inventory.BadgesComponent;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.UserBadgesMessageComposer;

public class GetSelectedBadgesMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int userId = this.packet.readInt();
        Habbo habbo = Emulator.getGameServer().getGameClientManager().getHabbo(userId);

        if (habbo == null || habbo.getHabboInfo() == null || habbo.getInventory() == null || habbo.getInventory().getBadgesComponent() == null)
            this.client.sendResponse(new UserBadgesMessageComposer(BadgesComponent.getBadgesOfflineHabbo(userId), userId));
        else
            this.client.sendResponse(new UserBadgesMessageComposer(habbo.getInventory().getBadgesComponent().getWearingBadges(), habbo.getHabboInfo().getId()));

        // Wired 2.0 trigger 24: this packet fires when a user clicks another user
        // (the 2016 client opens the infostand and requests badges on avatar click).
        Habbo clicker = this.client.getHabbo();
        if (clicker != null && habbo != null && clicker != habbo) {
            Room room = clicker.getHabboInfo().getCurrentRoom();
            if (room != null && habbo.getHabboInfo().getCurrentRoom() == room
                    && clicker.getRoomUnit() != null && habbo.getRoomUnit() != null) {
                WiredManager.triggerUserClicksUser(room, clicker.getRoomUnit(), habbo.getRoomUnit());
            }
        }
    }
}
