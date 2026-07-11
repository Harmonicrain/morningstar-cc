package com.eu.habbo.habbohotel.quests.listeners;

import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.habbohotel.quests.QuestType;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.plugin.events.furniture.FurnitureMovedEvent;
import com.eu.habbo.plugin.events.furniture.FurniturePlacedEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureRotatedEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureToggleEvent;
import com.eu.habbo.plugin.events.navigator.NavigatorRoomCreatedEvent;
import com.eu.habbo.plugin.events.rooms.RoomPulseEvent;
import com.eu.habbo.plugin.events.users.UserAvatarExpressionEvent;
import com.eu.habbo.plugin.events.users.UserCraftProductEvent;
import com.eu.habbo.plugin.events.users.UserDanceEvent;
import com.eu.habbo.plugin.events.users.UserEnterRoomEvent;
import com.eu.habbo.plugin.events.users.UserFriendFurniLockedEvent;
import com.eu.habbo.plugin.events.users.UserGameBBLockTilesEvent;
import com.eu.habbo.plugin.events.users.UserGameEvent;
import com.eu.habbo.plugin.events.users.UserKickBallEvent;
import com.eu.habbo.plugin.events.users.UserPublishPictureEvent;
import com.eu.habbo.plugin.events.users.UserReceiveHandItemEvent;
import com.eu.habbo.plugin.events.users.UserRespectedEvent;
import com.eu.habbo.plugin.events.users.UserSavedLookEvent;
import com.eu.habbo.plugin.events.users.UserSavedMottoEvent;
import com.eu.habbo.plugin.events.users.UserSwimEvent;
import com.eu.habbo.plugin.events.users.UserTakeStepEvent;
import com.eu.habbo.plugin.events.users.UserTalkEvent;
import com.eu.habbo.plugin.events.users.UserTeleportEvent;
import com.eu.habbo.plugin.events.users.UserWearBadgeEvent;
import com.eu.habbo.plugin.events.users.catalog.UserCatalogItemPurchasedEvent;
import com.eu.habbo.plugin.events.users.friends.UserFollowFriendEvent;
import com.eu.habbo.plugin.events.users.friends.UserFriendChatEvent;
import com.eu.habbo.plugin.events.users.friends.UserInviteFriendEvent;
import com.eu.habbo.plugin.events.users.friends.UserRelationShipEvent;
import com.eu.habbo.plugin.events.users.friends.UserRequestFriendshipEvent;
import com.eu.habbo.plugin.events.users.pets.UserPetEatEvent;
import com.eu.habbo.plugin.events.users.pets.UserPetLevelEvent;
import com.eu.habbo.plugin.events.users.pets.UserPetRespectEvent;

public class QuestEventListener {

    public static void onUserEnterRoomEvent(UserEnterRoomEvent event) {
        if (event.habbo == null)
            return;

        if (event.room.getOwnerId() == event.habbo.getHabboInfo().getId())
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.ENTER_OTHER_USERS_ROOM);
    }

    public static void onRoomPulseEvent(RoomPulseEvent event) {
        if (event.habbo == null)
            return;

        if (event.room == null)
            return;

        if (event.habbo.getRoomUnit() == null)
            return;

        var tile = event.habbo.getRoomUnit().getCurrentLocation();

        if (tile == null)
            return;
        
        for (var item : event.room.getItemsAt(tile)) {
            if (item == null || item.getBaseItem() == null)
                continue;

            QuestManager.handleTrigger(event.habbo, QuestType.ROOM_PULSE, "base_item_id", String.valueOf(item.getBaseItem().getId()));
        }
    }

    public static void onUserSavedMottoEvent(UserSavedMottoEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.CHANGE_MOTTO);
    }

    public static void onUserTalkEvent(UserTalkEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.CHAT_WITH_SOMEONE);
    }

    public static void onUserSavedLookEvent(UserSavedLookEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.CHANGE_FIGURE);
    }

    public static void onUserReceiveHandItemEvent(UserReceiveHandItemEvent event) {
        if (event.habbo == null)
            return;

        if (event.handItemId <= 0)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.FIND_HAND_ITEM, "hand_item_id", String.valueOf(event.handItemId));
    }

    public static void onFurnitureToggleEvent(FurnitureToggleEvent event) {
        if (event.habbo == null)
            return;

        if (event.furniture == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.FIND_STUFF, "base_item_id", String.valueOf(event.furniture.getBaseItem().getId()));
        QuestManager.handleTrigger(event.habbo, QuestType.SWITCH_ITEM_STATE, "base_item_id", String.valueOf(event.furniture.getBaseItem().getId()));
    }

    public static void onUserRespectedEvent(UserRespectedEvent event) {
        if (event.habbo == null)
            return;
       
        QuestManager.handleTrigger(event.from, QuestType.GIVE_RESPECT);
    }

    public static void onUserWearBadgeEvent(UserWearBadgeEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.WEAR_BADGE, "badge_code", event.badge.getCode());
    }

    public static void onUserAvatarExpressionEvent(UserAvatarExpressionEvent event) {
        if (event.habbo == null)
            return;

        if (event.action == null)
            return;

        switch (event.action) {
            case WAVE:
                QuestManager.handleTrigger(event.habbo, QuestType.WAVE);
                break;

            default:
                break;
        }
    }

    public static void onUserDanceEvent(UserDanceEvent event) {
        if (event.habbo == null)
            return;

        if(event.danceType == null)
            return;

        if(event.danceType == DanceType.NONE)
            return;
        
        QuestManager.handleTrigger(event.habbo, QuestType.DANCE);
    }

    public static void onFurniturePlacedEvent(FurniturePlacedEvent event) {
        if (event.habbo == null)
            return;

        if (event.furniture == null)
            return;

        if (event.furniture.getBaseItem() == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.PLACE_ITEM, "base_item_id", String.valueOf(event.furniture.getBaseItem().getId()));
    }
    
    public static void onFurnitureMovedEvent(FurnitureMovedEvent event) {
        if (event.habbo == null)
            return;

        if (event.furniture == null)
            return;

        if (event.furniture.getBaseItem() == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.MOVE_ITEM, "base_item_id", String.valueOf(event.furniture.getBaseItem().getId()));
    }

    public static void onFurnitureRotatedEvent(FurnitureRotatedEvent event) {
        if (event.habbo == null)
            return;

        if (event.furniture == null)
            return;

        if (event.furniture.getBaseItem() == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.ROTATE_ITEM, "base_item_id", String.valueOf(event.furniture.getBaseItem().getId()));
    }

    public static void onUserCatalogItemPurchasedEvent(UserCatalogItemPurchasedEvent event){
         if (event.habbo == null)
            return;

        if (event.catalogItem == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.BUY_FROM_CATALOGUE, "catalog_item_id", String.valueOf(event.catalogItem.getId()));
    }

    public static void onUserWalkEvent(UserTakeStepEvent event) {
        if (event.habbo == null)
            return;

        if (event.habbo.getRoomUnit() == null)
            return;

        if (event.habbo.getRoomUnit().getRoom() == null)
            return;

        var room = event.habbo.getRoomUnit().getRoom();
        var tile = event.habbo.getRoomUnit().getCurrentLocation();

        if (tile == null)
            return;

        for (var item : room.getItemsAt(tile)) {
            if (item == null || item.getBaseItem() == null)
                continue;

            QuestManager.handleTrigger(event.habbo, QuestType.WALK_OVER_STUFF, "base_item_id", String.valueOf(item.getBaseItem().getId()));
        }
    }

    public static void onUserCraftProductEvent(UserCraftProductEvent event) {
        if (event.habbo == null)
            return;

        if (event.reward == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.CRAFT_PRODUCT, "craft_reward", String.valueOf(event.reward.getId()));
    }

    public static void onUserRequestFriendshipEvent(UserRequestFriendshipEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.REQUEST_FRIEND);
    }

    public static void onUserFriendChatEvent(UserFriendChatEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.SEND_MESSENGER_MESSAGE);
    }

    public static void onUserInviteFriendEvent(UserInviteFriendEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.SEND_MESSENGER_INVITE);
    }

    public static void onUserFollowFriendEvent(UserFollowFriendEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.FOLLOW_FRIEND);
    }

    public static void onUserRelationShipEvent(UserRelationShipEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.SET_RELATIONSHIP_STATUS, "relation_ship", String.valueOf(event.relationShip));
    }

    public static void onUserTeleportEvent(UserTeleportEvent event) {
        if (event.habbo == null)
            return;

        if (event.item == null || event.item.getBaseItem() == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.TELEPORT);
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.TELEPORT, "base_item_id", String.valueOf(event.item.getBaseItem().getId()));
    }

    public static void onUserPetRespectEvent(UserPetRespectEvent event) {
        if (event.habbo == null)
            return;

        if (event.pet == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.PET_RESPECT);
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.PET_RESPECT, "pet_id", String.valueOf(event.pet.getId()));
    }
    
    public static void onUserPetLevelEvent(UserPetLevelEvent event) {
        if (event.habbo == null)
            return;

        if (event.pet == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.PET_LEVEL);
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.PET_LEVEL, "level", String.valueOf(event.level));
    }

    public static void onUserPetEatEvent(UserPetEatEvent event) {
        if (event.habbo == null)
            return;

        if (event.pet == null)
            return;

        if (event.item == null || event.item.getBaseItem() == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.PET_EAT);
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.PET_EAT, "base_item_id", String.valueOf(event.item.getBaseItem().getId()));
    }

    public static void onUserKickBallEvent(UserKickBallEvent event) {
        if (event.habbo == null)
            return;

        if (event.ball == null || event.ball.getBaseItem() == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.KICK_BALL);
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.KICK_BALL, "base_item_id", String.valueOf(event.ball.getBaseItem().getId()));
    }

    public static void onUserSwimEvent(UserSwimEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.SWIM);
    }

    public static void onNavigatorRoomCreatedEvent(NavigatorRoomCreatedEvent event) {
        if (event.habbo == null)
            return;

        if (event.room == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.CREATE_ROOM);
            return;
        }

        if (event.room.getLayout() == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.CREATE_ROOM);
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.CREATE_ROOM, "model", event.room.getLayout().getName());
    }

    public static void onUserFriendFurniLockedEvent(UserFriendFurniLockedEvent event) {
        if (event.habbo == null)
            return;

        if (event.item == null || event.item.getBaseItem() == null) {
            QuestManager.handleTrigger(event.habbo, QuestType.FRIEND_FURNI_LOCKED);

            if (event.target != null)
                QuestManager.handleTrigger(event.target, QuestType.FRIEND_FURNI_LOCKED);

            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.FRIEND_FURNI_LOCKED, "base_item_id", String.valueOf(event.item.getBaseItem().getId()));

        if (event.target != null) 
            QuestManager.handleTrigger(event.target, QuestType.FRIEND_FURNI_LOCKED, "base_item_id", String.valueOf(event.item.getBaseItem().getId()));
    }

    public static void onUserGameBBLockTilesEvent(UserGameBBLockTilesEvent event) {
        if (event.habbo == null)
            return;

        if (event.area) {
            QuestManager.handleTrigger(event.habbo, QuestType.GAME_BB_LOCK_TILES, "tile_count_min", String.valueOf(event.tileCount));
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.GAME_BB_LOCK_TILE, event.tileCount);
    }

    public static void onUserGameEvent(UserGameEvent event) {
        if (event.habbo == null)
            return;

        if (event.gameType == null || event.gameType.isEmpty())
            return;

        if (event.won) {
            QuestManager.handleTrigger(event.habbo, QuestType.GAME_WIN_GAME, "game_type", event.gameType);
            return;
        }

        QuestManager.handleTrigger(event.habbo, QuestType.GAME_PLAY_GAME, "game_type", event.gameType);
    }

    //need to test this one
    public static void onUserPublishPictureEvent(UserPublishPictureEvent event) {
        if (event.habbo == null)
            return;

        QuestManager.handleTrigger(event.habbo, QuestType.PUBLISH_PICTURE);
    }
}