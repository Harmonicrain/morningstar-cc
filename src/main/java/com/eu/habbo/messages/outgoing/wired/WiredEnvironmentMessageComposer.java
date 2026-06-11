package com.eu.habbo.messages.outgoing.wired;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class WiredEnvironmentMessageComposer extends MessageComposer {
    private final Room room;

    public WiredEnvironmentMessageComposer(Room room) {
        this.room = room;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.WiredEnvironmentMessageComposer);
        this.response.appendBoolean(this.room != null
                && !this.room.getRoomSpecialTypes().getTriggers(WiredTriggerType.CLICK_USER).isEmpty());
        this.response.appendInt(0); // May appends enabled wired achievement ids here.
        return this.response;
    }
}
