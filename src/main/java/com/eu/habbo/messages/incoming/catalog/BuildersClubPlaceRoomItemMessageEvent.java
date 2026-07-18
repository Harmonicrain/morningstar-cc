package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.habbohotel.items.FurnitureType;
import com.eu.habbo.habbohotel.rooms.FurnitureMovementError;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomState;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.BCPlacementWarningMessageComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.generic.alerts.NotificationDialogMessageComposer;

public class BuildersClubPlaceRoomItemMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int pageId = this.packet.readInt();
        int offerId = this.packet.readInt();
        String extraData = this.packet.readString();
        short x = this.packet.readInt().shortValue();
        short y = this.packet.readInt().shortValue();
        int rotation = RoomLayout.normalizeRotation(this.packet.readInt());
        boolean confirmHideRoom = this.packet.readBoolean();

        // Check trial warning condition before validate() reserves a slot.
        var earlyRoom = this.client.getHabbo().getHabboInfo().getCurrentRoom();
        boolean shouldWarnTrial = !confirmHideRoom
                && earlyRoom != null
                && this.client.getHabbo().getHabboStats().isOnBuildersClubFreeTrial()
                && !earlyRoom.hasUserBuildersClubItems(this.client.getHabbo().getHabboInfo().getId());

        BuildersClubPlacementSupport.ValidatedPlacement placement = BuildersClubPlacementSupport.validate(this.client, pageId, offerId, FurnitureType.FLOOR);
        if (placement == null) {
            return;
        }

        // validate() reserved an in-flight slot on success; release it on every path below.
        boolean released = false;
        try {
            if (shouldWarnTrial) {
                // Release the slot immediately — item is not being placed yet.
                // The client will resend with confirmHideRoom=true after the user clicks OK.
                this.client.getHabbo().getHabboStats().releaseBuildersClubSlot();
                released = true;
                this.client.sendResponse(new BCPlacementWarningMessageComposer(
                        pageId, offerId, extraData, x, y, rotation));
                return;
            }

            RoomTile tile = placement.room().getLayout().getTile(x, y);
            if (tile == null) {
                this.client.sendResponse(new NotificationDialogMessageComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "builders_club.invalid_tile"));
                return;
            }

            HabboItem item = BuildersClubPlacementSupport.createBuildersClubItem(this.client.getHabbo(), placement.baseItem(), extraData);
            if (item == null) {
                this.client.sendResponse(new NotificationDialogMessageComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, "builders_club.create_failed"));
                return;
            }

            FurnitureMovementError error = placement.room().canPlaceFurnitureAt(item, this.client.getHabbo(), tile, rotation);
            if (!error.equals(FurnitureMovementError.NONE)) {
                com.eu.habbo.Emulator.getGameEnvironment().getItemManager().deleteItem(item);
                this.client.getHabbo().getHabboStats().invalidateBuildersClubFurniCount();
                this.client.sendResponse(new NotificationDialogMessageComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, error.errorCode));
                return;
            }

            error = placement.room().placeFloorFurniAt(item, tile, rotation, this.client.getHabbo());
            if (!error.equals(FurnitureMovementError.NONE)) {
                com.eu.habbo.Emulator.getGameEnvironment().getItemManager().deleteItem(item);
                this.client.getHabbo().getHabboStats().invalidateBuildersClubFurniCount();
                this.client.sendResponse(new NotificationDialogMessageComposer(BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key, error.errorCode));
                return;
            }

            // Release the reservation first so sendUpdatedState sees the fresh post-commit count.
            this.client.getHabbo().getHabboStats().releaseBuildersClubSlot();
            released = true;
            BuildersClubPlacementSupport.sendUpdatedState(this.client.getHabbo());

            // Hide the room from the navigator on the first confirmed BC item placement by a trial user.
            if (confirmHideRoom && this.client.getHabbo().getHabboStats().isOnBuildersClubFreeTrial()
                    && placement.room().getState() != RoomState.INVISIBLE) {
                placement.room().setState(RoomState.INVISIBLE);
                placement.room().setNeedsUpdate(true);
                placement.room().save();
                this.client.sendResponse(new NotificationDialogMessageComposer(BubbleAlertKeys.BUILDERS_CLUB_ROOM_LOCKED.key));
            }
        } finally {
            if (!released) {
                this.client.getHabbo().getHabboStats().releaseBuildersClubSlot();
            }
        }
    }
}
