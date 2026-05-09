package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Rank;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.navigator.NavigatorSearchResultEvent;
import gnu.trove.map.hash.THashMap;

import java.util.ArrayList;

public class RoomTextSearchMessageEvent extends MessageHandler {
    public final static THashMap<Rank, THashMap<String, ServerMessage>> cachedResults = new THashMap<>(4);

    @Override
    public void handle() throws Exception {
        String name = this.packet.readString();

        String prefix = "";
        String query = name;
        ArrayList<Room> rooms;

        if (this.client.getHabbo().getHabboStats().isNewNavigatorEnabled()) {
            if (name.startsWith("owner:")) {
                query = name.substring("owner:".length());
                prefix = "owner:";
                rooms = (ArrayList<Room>) Emulator.getGameEnvironment().getRoomManager().getRoomsForHabbo(query);
            } else if (name.startsWith("tag:")) {
                query = name.substring("tag:".length());
                prefix = "tag:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithTag(query);
            } else if (name.startsWith("group:")) {
                query = name.substring("group:".length());
                prefix = "group:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getGroupRoomsWithName(query);
            } else if (name.startsWith("roomname:")) {
                query = name.substring("roomname:".length());
                prefix = "roomname:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithName(query);
            } else {
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithName(name);
            }

            NavigatorSearchResultEvent event = new NavigatorSearchResultEvent(this.client.getHabbo(), prefix, query, rooms);
            if (Emulator.getPluginManager().fireEvent(event).isCancelled()) {
                return;
            }

            NavigatorMixedModeSearchHelper.send(this.client, rooms, "query", name);
            return;
        }

        ServerMessage message = null;
        if (cachedResults.containsKey(this.client.getHabbo().getHabboInfo().getRank())) {
            message = cachedResults.get(this.client.getHabbo().getHabboInfo().getRank()).get((name + "\t" + query).toLowerCase());
        } else {
            cachedResults.put(this.client.getHabbo().getHabboInfo().getRank(), new THashMap<>());
        }

        if (message == null) {
            if (name.startsWith("owner:")) {
                query = name.substring("owner:".length());
                prefix = "owner:";
                rooms = (ArrayList<Room>) Emulator.getGameEnvironment().getRoomManager().getRoomsForHabbo(query);
            } else if (name.startsWith("tag:")) {
                query = name.substring("tag:".length());
                prefix = "tag:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithTag(query);
            } else if (name.startsWith("group:")) {
                query = name.substring("group:".length());
                prefix = "group:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getGroupRoomsWithName(query);
            } else if (name.startsWith("roomname:")) {
                query = name.substring("roomname:".length());
                prefix = "roomname:";
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithName(query);
            } else {
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithName(name);
            }

            message = NavigatorMixedModeSearchHelper.composeLegacy(rooms);
            THashMap<String, ServerMessage> map = cachedResults.get(this.client.getHabbo().getHabboInfo().getRank());

            if (map == null) {
                map = new THashMap<>(1);
            }

            map.put((name + "\t" + query).toLowerCase(), message);
            cachedResults.put(this.client.getHabbo().getHabboInfo().getRank(), map);

            NavigatorSearchResultEvent event = new NavigatorSearchResultEvent(this.client.getHabbo(), prefix, query, rooms);
            if (Emulator.getPluginManager().fireEvent(event).isCancelled()) {
                return;
            }
        }

        this.client.sendResponse(message);
    }
}
