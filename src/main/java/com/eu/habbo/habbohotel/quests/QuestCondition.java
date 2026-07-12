package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

import java.sql.ResultSet;
import java.sql.SQLException;

public class QuestCondition {
    private final int id;
    private final int questId;
    private final QuestConditionType conditionType;
    private final String conditionKey;
    private final String conditionValue;
    private final String conditionExtra;

    public QuestCondition(ResultSet set) throws SQLException {
        this.id = set.getInt("id");
        this.questId = set.getInt("quest_id");
        this.conditionType = QuestConditionType.fromString(set.getString("condition_type"));
        this.conditionKey = set.getString("condition_key");
        this.conditionValue = set.getString("condition_value");
        this.conditionExtra = set.getString("condition_extra");
    }

    public int getId() {
        return this.id;
    }

    public int getQuestId() {
        return this.questId;
    }

    public QuestConditionType getConditionType() {
        return this.conditionType;
    }

    public boolean matches(Habbo habbo) {
        if (habbo == null || this.conditionType == null) {
            return false;
        }

        switch (this.conditionType) {
            case FIGURE_PART:
                return this.matchesFigurePart(habbo);

            case ROOM_ITEM_STATE:
                return this.matchesRoomItemState(habbo);

            default:
                return false;
        }
    }

    private boolean matchesFigurePart(Habbo habbo) {
        if (this.conditionKey == null || this.conditionKey.isEmpty()) {
            return false;
        }

        if (this.conditionValue == null || this.conditionValue.isEmpty()) {
            return false;
        }

        if (habbo.getHabboInfo() == null || habbo.getHabboInfo().getLook() == null) {
            return false;
        }

        String wantedPrefix = this.conditionKey + "-";

        for (String part : habbo.getHabboInfo().getLook().split("\\.")) {
            if (!part.startsWith(wantedPrefix)) {
                continue;
            }

            String[] data = part.split("-");

            if (data.length < 2) {
                continue;
            }

            if (data[1].equalsIgnoreCase(this.conditionValue)) {
                return true;
            }
        }

        return false;
    }

    private boolean matchesRoomItemState(Habbo habbo) {
        if (habbo.getHabboInfo() == null) {
            return false;
        }

        Room room = habbo.getHabboInfo().getCurrentRoom();

        if (room == null) {
            return false;
        }

        if (this.conditionKey == null || this.conditionValue == null || this.conditionExtra == null) {
            return false;
        }

        for (HabboItem item : room.getFloorItems()) {
            if (item == null || item.getBaseItem() == null) {
                continue;
            }

            if (this.conditionKey.equalsIgnoreCase("base_item_id")) {
                if (!String.valueOf(item.getBaseItem().getId()).equalsIgnoreCase(this.conditionValue)) {
                    continue;
                }
            } else if (this.conditionKey.equalsIgnoreCase("item_id")) {
                if (!String.valueOf(item.getId()).equalsIgnoreCase(this.conditionValue)) {
                    continue;
                }
            } else {
                return false;
            }

            if (item.getExtradata() != null && item.getExtradata().equalsIgnoreCase(this.conditionExtra)) {
                return true;
            }
        }

        return false;
    }
}