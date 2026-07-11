package com.eu.habbo.habbohotel.quests;

public enum QuestConditionType {
    FIGURE_PART,
    ROOM_ITEM_STATE;

    public static QuestConditionType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return QuestConditionType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}