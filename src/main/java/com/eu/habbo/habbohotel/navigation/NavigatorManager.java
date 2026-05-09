package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NavigatorManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NavigatorManager.class);

    public static int MAXIMUM_RESULTS_PER_PAGE = 10;
    public static boolean CATEGORY_SORT_USING_ORDER_NUM = false;

    public int officialRootCategoryId = -1;
    public int staffPicksCategoryId = -1;
    public final THashMap<Integer, NavigatorPublicCategory> publicCategories = new THashMap<>();
    public final ConcurrentHashMap<String, NavigatorFilterField> filterSettings = new ConcurrentHashMap<>();
    public final THashMap<String, NavigatorFilter> filters = new THashMap<>();

    public NavigatorManager() {
        long millis = System.currentTimeMillis();

        this.filters.put(NavigatorPublicFilter.name, new NavigatorPublicFilter());
        this.filters.put(NavigatorHotelFilter.name, new NavigatorHotelFilter());
        this.filters.put(NavigatorRoomAdsFilter.name, new NavigatorRoomAdsFilter());
        this.filters.put(NavigatorUserFilter.name, new NavigatorUserFilter());
        this.filters.put(NavigatorFavoriteFilter.name, new NavigatorFavoriteFilter());

        LOGGER.info("Navigator Manager -> Loaded! ({} MS)", System.currentTimeMillis() - millis);
    }

    public void refreshNavigatorData() {
        this.loadNavigator();
    }

    public void loadNavigator() {
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            synchronized (this.publicCategories) {
                this.clearPublicCategories();
                this.officialRootCategoryId = Emulator.getConfig().getInt("hotel.navigator.officialroot.categoryid", -1);
                this.staffPicksCategoryId = Emulator.getConfig().getInt("hotel.navigator.staffpicks.categoryid", -1);

                try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM navigator_publiccats WHERE visible = '1' ORDER BY order_num DESC")) {
                    while (set.next()) {
                        this.publicCategories.put(set.getInt("id"), new NavigatorPublicCategory(set));
                    }
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }

                try (Statement statement = connection.createStatement(); ResultSet set = statement.executeQuery("SELECT * FROM navigator_publics WHERE visible = '1'")) {
                    while (set.next()) {
                        NavigatorPublicCategory category = this.publicCategories.get(set.getInt("public_cat_id"));

                        if (category != null) {
                            Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(set.getInt("room_id"));

                            if (room != null) {
                                category.addRoom(room);
                            } else {
                                LOGGER.error("Public room (ID: {} defined in navigator_publics does not exist!", set.getInt("room_id"));
                            }
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.error("Caught SQL exception", e);
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }

        this.loadFilterSettings();
    }

    private void loadFilterSettings() {
        synchronized (this.filterSettings) {
            this.filterSettings.clear();

            this.addFilterSetting("anything", "filterAnything", NavigatorFilterComparator.CONTAINS,
                    "SELECT rooms.*, CONCAT_WS(' ', rooms.owner_name, rooms.name, rooms.description, rooms.tags, guilds.name, guilds.description, room_promotions.title, room_promotions.description) AS whole " +
                            "FROM rooms " +
                            "LEFT JOIN guilds ON rooms.guild_id = guilds.id " +
                            "LEFT JOIN room_promotions ON rooms.id = room_promotions.room_id AND room_promotions.end_timestamp >= UNIX_TIMESTAMP() " +
                            "HAVING whole LIKE ?");
            this.addFilterSetting("desc", "getDescription", NavigatorFilterComparator.CONTAINS,
                    "SELECT * FROM rooms WHERE description LIKE ?");
            this.addFilterSetting("group", "getGuildName", NavigatorFilterComparator.CONTAINS,
                    "SELECT rooms.* FROM rooms INNER JOIN guilds ON rooms.guild_id = guilds.id WHERE CONCAT(guilds.name, guilds.description) LIKE ?");
            this.addFilterSetting("owner", "getOwnerName", NavigatorFilterComparator.EQUALS_IGNORE_CASE,
                    "SELECT * FROM rooms WHERE owner_name LIKE ?");
            this.addFilterSetting("promo", "getPromotionDesc", NavigatorFilterComparator.CONTAINS,
                    "SELECT rooms.* FROM rooms INNER JOIN room_promotions ON rooms.id = room_promotions.room_id WHERE room_promotions.end_timestamp >= UNIX_TIMESTAMP() AND CONCAT(room_promotions.title, room_promotions.description) LIKE ?");
            this.addFilterSetting("roomname", "getName", NavigatorFilterComparator.CONTAINS,
                    "SELECT * FROM rooms WHERE name COLLATE UTF8_GENERAL_CI LIKE ?");
            this.addFilterSetting("tag", "getTags", NavigatorFilterComparator.EQUALS,
                    "SELECT * FROM rooms WHERE CONCAT(';', tags) LIKE CONCAT('%;', ?, ';%')");
        }
    }

    private void addFilterSetting(String key, String fieldName, NavigatorFilterComparator comparator, String databaseQuery) {
        try {
            this.filterSettings.put(key, new NavigatorFilterField(key, Room.class.getDeclaredMethod(fieldName), databaseQuery, comparator));
        } catch (NoSuchMethodException e) {
            LOGGER.error("Could not register navigator filter field {}", fieldName, e);
        }
    }

    private void clearPublicCategories() {
        List<NavigatorPublicCategory> categories = new ArrayList<>(this.publicCategories.values());

        for (NavigatorPublicCategory category : categories) {
            List<Room> rooms = new ArrayList<>(category.rooms);

            for (Room room : rooms) {
                category.removeRoom(room);
            }
        }

        this.publicCategories.clear();
    }

    public void removeDeletedRoom(Room room) {
        if (room == null) {
            return;
        }

        if (room.isPublicRoom() || room.isStaffPicked() || this.isInPublicCategory(room)) {
            synchronized (this.publicCategories) {
                for (NavigatorPublicCategory category : new ArrayList<>(this.publicCategories.values())) {
                    if (category.rooms.contains(room)) {
                        category.removeRoom(room);
                    }
                }
            }
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM navigator_publics WHERE room_id = ?")) {
            statement.setInt(1, room.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("Caught SQL exception", e);
        }
    }

    private boolean isInPublicCategory(Room room) {
        synchronized (this.publicCategories) {
            for (NavigatorPublicCategory category : this.publicCategories.values()) {
                if (category.rooms.contains(room)) {
                    return true;
                }
            }
        }

        return false;
    }

    public NavigatorFilterComparator comperatorForField(Method field) {
        for (Map.Entry<String, NavigatorFilterField> set : this.filterSettings.entrySet()) {
            if (set.getValue().field == field) {
                return set.getValue().comparator;
            }
        }

        return null;
    }

    public List<Room> getRoomsForCategory(String category, Habbo habbo) {
        List<Room> rooms = new ArrayList<>();

        switch (category) {
            case "my":
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsForHabbo(habbo);
                break;
            case "favorites":
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsFavourite(habbo);
                break;
            case "history_freq":
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsVisited(habbo, false, 10);
                break;
            case "my_groups":
                rooms = Emulator.getGameEnvironment().getRoomManager().getGroupRooms(habbo, 25);
                break;
            case "with_rights":
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithRights(habbo);
                break;
            case "popular":
                rooms = Emulator.getGameEnvironment().getRoomManager().getPopularRooms(Emulator.getConfig().getInt("hotel.navigator.popular.amount"));
                break;
            case "categories":
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsPromoted();
                break;
            case "with_friends":
                rooms = Emulator.getGameEnvironment().getRoomManager().getRoomsWithFriendsIn(habbo, 25);
                break;
            case "highest_score":
                rooms = Emulator.getGameEnvironment().getRoomManager().getTopRatedRooms(25);
                break;
            default:
                return null;
        }

        Collections.sort(rooms);

        return rooms;
    }
}
