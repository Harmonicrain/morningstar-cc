package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.RoomForwardMessageComposer;

import java.util.ArrayList;
import java.util.Collections;

public class ForwardToARandomPromotedRoomMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        ArrayList<Room> rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsPromoted();

        if (!rooms.isEmpty()) {
            Collections.shuffle(rooms);
            this.client.sendResponse(new RoomForwardMessageComposer(rooms.get(0).getId()));
            return;
        }

        int noobLobbyRoomId = Emulator.getConfig().getInt("hotel.room.nooblobby");

        if (noobLobbyRoomId > 0) {
            this.client.sendResponse(new RoomForwardMessageComposer(noobLobbyRoomId));
        }
    }
}
