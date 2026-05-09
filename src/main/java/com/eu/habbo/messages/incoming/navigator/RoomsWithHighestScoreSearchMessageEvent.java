package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;

import java.util.ArrayList;
import java.util.List;

public class RoomsWithHighestScoreSearchMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int category = this.packet.bytesAvailable() >= 4 ? this.packet.readInt() : -1;
        List<Room> rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsByScore();

        if (category >= 0) {
            List<Room> filteredRooms = new ArrayList<>();
            for (Room room : rooms) {
                if (room.getCategory() == category) {
                    filteredRooms.add(room);
                }
            }
            rooms = filteredRooms;
        }

        String query = category >= 0 ? String.valueOf(category) : "";
        NavigatorMixedModeSearchHelper.send(this.client, rooms, "highest_score", query);
    }
}
