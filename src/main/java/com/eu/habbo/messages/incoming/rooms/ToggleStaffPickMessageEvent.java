package com.eu.habbo.messages.incoming.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.navigation.NavigatorPublicCategory;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.GetGuestRoomResultMessageComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ToggleStaffPickMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().hasPermission(Permission.ACC_STAFF_PICK)) {
            int roomId = this.packet.readInt();

            Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(roomId);

            if (room != null) {
                int staffPicksCategoryId = Emulator.getConfig().getInt("hotel.navigator.staffpicks.categoryid", -1);
                NavigatorPublicCategory publicCategory = Emulator.getGameEnvironment().getNavigatorManager().publicCategories.get(staffPicksCategoryId);

                if (staffPicksCategoryId != -1) {
                    boolean isStaffPicked;

                    try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                         PreparedStatement existsStatement = connection.prepareStatement("SELECT 1 FROM navigator_publics WHERE public_cat_id = ? AND room_id = ? AND visible = '1' LIMIT 1");
                         PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO navigator_publics (public_cat_id, room_id, visible) VALUES (?, ?, '1') ON DUPLICATE KEY UPDATE visible = '1'");
                         PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM navigator_publics WHERE public_cat_id = ? AND room_id = ?")) {
                        existsStatement.setInt(1, staffPicksCategoryId);
                        existsStatement.setInt(2, room.getId());

                        try (ResultSet set = existsStatement.executeQuery()) {
                            isStaffPicked = set.next();
                        }

                        if (isStaffPicked) {
                            deleteStatement.setInt(1, staffPicksCategoryId);
                            deleteStatement.setInt(2, room.getId());
                            deleteStatement.executeUpdate();

                            if (publicCategory != null) {
                                publicCategory.removeRoom(room);
                            }
                        } else {
                            insertStatement.setInt(1, staffPicksCategoryId);
                            insertStatement.setInt(2, room.getId());
                            insertStatement.executeUpdate();

                            Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(room.getOwnerId());

                            if (habbo != null) {
                                AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("Spr"));
                            }

                            if (publicCategory != null) {
                                publicCategory.addRoom(room);
                            }
                        }
                    }
                }

                this.client.sendResponse(new GetGuestRoomResultMessageComposer(room, this.client.getHabbo(), true, false));
            }
        }
    }
}
