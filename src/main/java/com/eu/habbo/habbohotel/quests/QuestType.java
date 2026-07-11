package com.eu.habbo.habbohotel.quests;

public enum QuestType {
    // implemented
    ENTER_OTHER_USERS_ROOM,
    FIND_HAND_ITEM,
    ROOM_PULSE,
    SWITCH_ITEM_STATE,
    CHANGE_MOTTO,
    CHANGE_FIGURE,
    CHAT_WITH_SOMEONE,
    GIVE_RESPECT,
    WEAR_BADGE,
    WAVE,
    DANCE,
    FIND_STUFF,
    PLACE_ITEM,
    MOVE_ITEM,
    ROTATE_ITEM,
    BUY_FROM_CATALOGUE,
    WALK_OVER_STUFF,
    CRAFT_PRODUCT,
    REQUEST_FRIEND,
    SEND_MESSENGER_MESSAGE,
    SEND_MESSENGER_INVITE,
    FOLLOW_FRIEND,
    SET_RELATIONSHIP_STATUS,
    TELEPORT,
    PET_RESPECT,
    PET_LEVEL,
    PET_EAT,
    KICK_BALL,
    SWIM,
    CREATE_ROOM,
    FRIEND_FURNI_LOCKED,
    GAME_BB_LOCK_TILES,
    GAME_BB_LOCK_TILE,
    GAME_PLAY_GAME,
    GAME_WIN_GAME,

    // need to test
    PUBLISH_PICTURE,

    // idk what this quest type do
    CUSTOM_WIRED_ACTION,
    
    // habbo new features
    PLACE_BUILDERS_CLUB_FURNI,
    REPLENISH_RESPECT,
    USE_HABBICON;

    public static QuestType fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            return QuestType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
