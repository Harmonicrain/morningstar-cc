package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomManager;
import com.eu.habbo.messages.incoming.MessageHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Reuses GuestRoomSearchResultMessageComposer because the wire format is the
// same as a search response; the client tells the two apart by which request it sent.
public class MyRecommendedRoomsMessageEvent extends MessageHandler {
    private static final int MAX_RECOMMENDATIONS = 50;

    @Override
    public void handle() throws Exception {
        List<Room> rooms = new ArrayList<>();
        for (Room room : Emulator.getGameEnvironment().getRoomManager().getActiveRooms(RoomManager.CATEGORY_ANY)) {
            if (!room.isPublicRoom()) {
                rooms.add(room);
            }
        }
        Collections.sort(rooms);
        if (rooms.size() > MAX_RECOMMENDATIONS) {
            rooms = rooms.subList(0, MAX_RECOMMENDATIONS);
        }
        NavigatorMixedModeSearchHelper.send(this.client, rooms, "recommended", "");
    }
}
