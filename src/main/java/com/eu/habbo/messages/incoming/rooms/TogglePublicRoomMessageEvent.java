package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.navigation.NavigatorPublicCategory;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.GetGuestRoomResultMessageComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TogglePublicRoomMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().hasPermission(Permission.ACC_PUBLIC_PICK)) {
            int roomId = this.packet.readInt();

            Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);

            if (room != null) {
                int publicRootCategoryId = Emulator.getConfig().getInt("hotel.navigator.officialroot.categoryid", -1);

                if (publicRootCategoryId != -1) {
                    boolean isPublicRoom;

                    try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                         PreparedStatement existsStatement = connection.prepareStatement("SELECT 1 FROM navigator_publics WHERE public_cat_id = ? AND room_id = ? AND visible = '1' LIMIT 1");
                         PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO navigator_publics (public_cat_id, room_id, visible) VALUES (?, ?, '1') ON DUPLICATE KEY UPDATE visible = '1'");
                         PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM navigator_publics WHERE public_cat_id = ? AND room_id = ?")) {
                        existsStatement.setInt(1, publicRootCategoryId);
                        existsStatement.setInt(2, room.getId());

                        try (ResultSet set = existsStatement.executeQuery()) {
                            isPublicRoom = set.next();
                        }

                        if (isPublicRoom) {
                            deleteStatement.setInt(1, publicRootCategoryId);
                            deleteStatement.setInt(2, room.getId());
                            deleteStatement.executeUpdate();

                            synchronized (Emulator.getGameEnvironment().getNavigatorManager().publicCategories) {
                                NavigatorPublicCategory publicCategory = Emulator.getGameEnvironment().getNavigatorManager().publicCategories.get(publicRootCategoryId);
                                if (publicCategory != null) {
                                    publicCategory.removeRoom(room);
                                }
                            }
                        } else {
                            insertStatement.setInt(1, publicRootCategoryId);
                            insertStatement.setInt(2, room.getId());
                            insertStatement.executeUpdate();

                            synchronized (Emulator.getGameEnvironment().getNavigatorManager().publicCategories) {
                                NavigatorPublicCategory publicCategory = Emulator.getGameEnvironment().getNavigatorManager().publicCategories.get(publicRootCategoryId);
                                if (publicCategory != null) {
                                    publicCategory.addRoom(room);
                                }
                            }
                        }
                    }
                }

                this.client.sendResponse(new GetGuestRoomResultMessageComposer(room, this.client.getHabbo(), true, false));
            }
        }
    }
}
