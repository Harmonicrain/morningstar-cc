package com.eu.habbo.messages.outgoing.rooms;

import com.eu.habbo.habbohotel.rooms.PublicItem;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class PublicRoomObjectsMessageComposer extends MessageComposer {
    private final Room room;
    private final List<PublicItem> items;

    public PublicRoomObjectsMessageComposer(Room room, List<PublicItem> items) {
        this.room = room;
        this.items = items;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.PublicRoomObjectsMessageComposer);
        this.response.appendInt(this.room.getId());
        this.response.appendInt(this.room.getCategory());
        this.response.appendInt(this.items.size());

        for (PublicItem item : this.items) {
            this.response.appendBoolean(item.hasDimensions());
            this.response.appendString(item.getId());
            this.response.appendString(item.getSprite());
            this.response.appendInt(item.getX());
            this.response.appendInt(item.getY());
            this.response.appendInt(item.getZ());

            if (item.hasDimensions()) {
                this.response.appendInt(item.getLength());
                this.response.appendInt(item.getWidth());
            } else {
                this.response.appendInt(item.getRotation());
            }
        }

        return this.response;
    }
}
