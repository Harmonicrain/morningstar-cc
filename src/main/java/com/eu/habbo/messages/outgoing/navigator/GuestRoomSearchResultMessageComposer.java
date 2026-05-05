package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GuestRoomSearchResultMessageComposer extends MessageComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuestRoomSearchResultMessageComposer.class);

    private final List<Room> rooms;
    private final boolean useLegacyTail;

    public GuestRoomSearchResultMessageComposer(List<Room> rooms) {
        this(rooms, true);
    }

    public GuestRoomSearchResultMessageComposer(List<Room> rooms, boolean useLegacyTail) {
        this.rooms = rooms;
        this.useLegacyTail = useLegacyTail;
    }

    @Override
    protected ServerMessage composeInternal() {
        try {
            this.response.init(Outgoing.GuestRoomSearchResultMessageComposer);

            this.response.appendInt(2);
            this.response.appendString("");

            this.response.appendInt(this.rooms.size());

            for (Room room : this.rooms) {
                room.serialize(this.response);
            }

            if (this.useLegacyTail) {
                this.response.appendBoolean(true);

                this.response.appendInt(0);
                this.response.appendString("A");
                this.response.appendString("B");
                this.response.appendInt(1);
                this.response.appendString("C");
                this.response.appendString("D");
                this.response.appendInt(1);
                this.response.appendInt(1);
                this.response.appendInt(1);
                this.response.appendString("E");
            } else {
                this.response.appendBoolean(false);
            }

            return this.response;
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
        return null;
    }

    public List<Room> getRooms() {
        return rooms;
    }
}
