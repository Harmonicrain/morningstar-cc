package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredSelector;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredCategoryType;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettingsNew;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wired.WiredSavedMessageComposer;

public class UpdateSelectorMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int visibleId = this.packet.readInt();
        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null) {
            return;
        }

        if (!(room.hasRights(this.client.getHabbo())
                || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId()
                || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER)
                || this.client.getHabbo().hasPermission(Permission.ACC_MOVEROTATE))) {
            return;
        }

        // Client sends the room-visible id (BC furni use virtual ids); resolve to db id.
        int itemId = room.getItemManager().resolveVisibleId(visibleId);
        InteractionWiredSelector selector = room.getRoomSpecialTypes().getSelector(itemId);
        if (selector == null) {
            return;
        }

        WiredSettingsNew settings = InteractionWired.readSettingsNew(this.packet, WiredCategoryType.SELECTOR, room);
        if (selector.saveData(settings)) {
            selector.setWiredSourceTypes(settings.getFurniSourceTypes(), settings.getUserSourceTypes());
            this.client.sendResponse(new WiredSavedMessageComposer());
            selector.needsUpdate(true);
            Emulator.getThreading().run(selector);
            WiredManager.invalidateRoom(room);
        }
    }
}
