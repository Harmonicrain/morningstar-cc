package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredSelector;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomSpecialTypes;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.wired.WiredConditionDataMessageComposer;
import com.eu.habbo.messages.outgoing.wired.WiredEffectDataMessageComposer;
import com.eu.habbo.messages.outgoing.wired.WiredSelectorDataMessageComposer;
import com.eu.habbo.messages.outgoing.wired.WiredTriggerDataMessageComposer;

/**
 * Wired 2.0 open/ack handshake (client -> server). The server's wired onClick now
 * sends WiredOpen(itemId); the client acks here with the same id, and the server
 * validates rights + resolves the wired element, then sends its data composer.
 *
 * Header 768 (0x0300) = the clean client's existing wired-open ack composer.
 */
public class OpenAckMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) {
            return;
        }

        boolean allowed = room.hasRights(this.client.getHabbo())
                || room.getOwnerId() == this.client.getHabbo().getHabboInfo().getId()
                || this.client.getHabbo().hasPermission(Permission.ACC_ANYROOMOWNER)
                || this.client.getHabbo().hasPermission(Permission.ACC_MOVEROTATE);
        if (!allowed) {
            return;
        }

        RoomSpecialTypes specialTypes = room.getRoomSpecialTypes();

        InteractionWiredTrigger trigger = specialTypes.getTrigger(itemId);
        if (trigger != null) {
            this.client.sendResponse(new WiredTriggerDataMessageComposer(trigger, room));
            return;
        }

        InteractionWiredEffect effect = specialTypes.getEffect(itemId);
        if (effect != null) {
            this.client.sendResponse(new WiredEffectDataMessageComposer(effect, room));
            return;
        }

        InteractionWiredCondition condition = specialTypes.getCondition(itemId);
        if (condition != null) {
            this.client.sendResponse(new WiredConditionDataMessageComposer(condition, room));
            return;
        }

        InteractionWiredSelector selector = specialTypes.getSelector(itemId);
        if (selector != null) {
            this.client.sendResponse(new WiredSelectorDataMessageComposer(selector, room));
        }
    }
}
