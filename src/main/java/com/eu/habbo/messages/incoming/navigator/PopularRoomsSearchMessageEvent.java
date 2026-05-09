package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;

import java.util.List;

public class PopularRoomsSearchMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String query = "";
        int category = -1;

        if (this.packet.bytesAvailable() > 0) {
            query = this.packet.readString();
        }

        if (this.packet.bytesAvailable() >= 4) {
            category = this.packet.readInt();
        }

        int selectedCategory = this.getSelectedCategory(query, category);

        List<Room> rooms = selectedCategory >= 0
                ? Emulator.getGameEnvironment().getRoomManager().getPopularRooms(Emulator.getConfig().getInt("hotel.navigator.popular.category.maxresults"), selectedCategory)
                : Emulator.getGameEnvironment().getRoomManager().getActiveRooms(-1);

        NavigatorMixedModeSearchHelper.send(this.client, rooms, "popular", query);
    }

    private int getSelectedCategory(String query, int category) {
        try {
            int categoryFromQuery = Integer.parseInt(query);
            if (categoryFromQuery >= 0) {
                return categoryFromQuery;
            }
        } catch (NumberFormatException ignored) {
        }

        return category;
    }
}
