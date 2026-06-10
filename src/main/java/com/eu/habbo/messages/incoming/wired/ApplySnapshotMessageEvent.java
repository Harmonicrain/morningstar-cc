package com.eu.habbo.messages.incoming.wired;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.wired.interfaces.InteractionWiredMatchFurniSettings;
import com.eu.habbo.habbohotel.rooms.FurnitureMovementError;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredMatchFurniSetting;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.generic.alerts.NotificationDialogMessageComposer;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertKeys;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ApplySnapshotMessageEvent extends MessageHandler {

    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        int itemId = this.packet.readInt();

        // Executing Habbo has to be in a Room
        if (!this.client.getHabbo().getRoomUnit().isInRoom()) {
            this.client.sendResponse(new NotificationDialogMessageComposer(
                    BubbleAlertKeys.FURNITURE_PLACEMENT_ERROR.key,
                    FurnitureMovementError.NO_RIGHTS.errorCode
            ));
            return;
        }

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room != null) {

            // Executing Habbo should be able to edit wireds
            if (room.hasRights(this.client.getHabbo()) || room.isOwner(this.client.getHabbo())) {

                List<HabboItem> wireds = new ArrayList<>();
                wireds.addAll(room.getRoomSpecialTypes().getConditions());
                wireds.addAll(room.getRoomSpecialTypes().getEffects());

                // Find the item with the given ID in the room
                Optional<HabboItem> item = wireds.stream()
                        .filter(wired -> wired.getId() == itemId)
                        .findFirst();

                // If the item exists
                if (item.isPresent()) {
                    HabboItem wiredItem = item.get();

                    // The item should have settings to match furni state, position and rotation
                    if (wiredItem instanceof InteractionWiredMatchFurniSettings) {

                        InteractionWiredMatchFurniSettings wired = (InteractionWiredMatchFurniSettings) wiredItem;

                        // Try to apply the set settings to each item
                        List<WiredMatchFurniSetting> settings = new ArrayList<>(wired.getMatchFurniSettings());
                        if (wired.shouldMatchAltitude()) {
                            settings.sort(Comparator.comparingDouble(setting -> setting.z));
                        } else if (wired.shouldMatchPosition()) {
                            settings.sort(Comparator
                                    .comparingDouble((WiredMatchFurniSetting setting) -> this.distanceToSavedTile(room, setting))
                                    .thenComparingDouble(setting -> this.currentZ(room, setting)));
                        }

                        settings.forEach(setting -> {
                            HabboItem matchItem = room.getHabboItemByDatabaseId(setting.item_id);
                            if (matchItem == null) {
                                return;
                            }

                            // Match state
                            if (wired.shouldMatchState() && matchItem.allowWiredResetState()) {
                                if (!setting.state.equals(" ") && !matchItem.getExtradata().equals(setting.state)) {
                                    matchItem.setExtradata(setting.state);
                                    room.updateItemState(matchItem);
                                }
                            }

                            RoomTile oldLocation = room.getLayout().getTile(matchItem.getX(), matchItem.getY());
                            double oldZ = matchItem.getZ();

                            boolean forceUpdate = false;
                            boolean animatedAltitudeMove = false;

                            // Match Position & Rotation
                            if(wired.shouldMatchRotation() && !wired.shouldMatchPosition()) {
                                if(matchItem.getRotation() != setting.rotation && room.furnitureFitsAt(oldLocation, matchItem, setting.rotation, false) == FurnitureMovementError.NONE) {
                                    room.moveFurniTo(matchItem, oldLocation, setting.rotation, null, true);
                                    forceUpdate = wired.shouldMatchAltitude();
                                }
                            }
                            else if(wired.shouldMatchPosition()) {
                                boolean slideAnimation = !wired.shouldMatchAltitude() && (!wired.shouldMatchRotation() || matchItem.getRotation() == setting.rotation);
                                RoomTile newLocation = room.getLayout().getTile((short) setting.x, (short) setting.y);
                                int newRotation = wired.shouldMatchRotation() ? setting.rotation : matchItem.getRotation();
                                boolean shouldMove = newLocation != oldLocation || newRotation != matchItem.getRotation() || !wired.shouldMatchAltitude()
                                        || (wired.shouldMatchAltitude() && matchItem.getZ() != setting.z);

                                if(newLocation != null && newLocation.state != RoomTileState.INVALID && shouldMove && room.furnitureFitsAt(newLocation, matchItem, newRotation, !wired.shouldMatchAltitude()) == FurnitureMovementError.NONE) {
                                    if(room.moveFurniTo(matchItem, newLocation, newRotation, null, !wired.shouldMatchAltitude() && !slideAnimation, !wired.shouldMatchAltitude()) == FurnitureMovementError.NONE) {
                                        forceUpdate = wired.shouldMatchAltitude();
                                        if(wired.shouldMatchAltitude()) {
                                            matchItem.setZ(setting.z);
                                            matchItem.needsUpdate(true);
                                            Emulator.getThreading().run(matchItem);
                                            room.updateTiles(room.getLayout().getTilesAt(newLocation, matchItem.getBaseItem().getWidth(),
                                                    matchItem.getBaseItem().getLength(), matchItem.getRotation()));
                                            room.sendComposer(new FloorItemOnRollerComposer(matchItem, null, oldLocation, oldZ, newLocation, setting.z, 0, room).compose());
                                            animatedAltitudeMove = true;
                                        } else if(slideAnimation) {
                                            room.sendComposer(new FloorItemOnRollerComposer(matchItem, null, oldLocation, oldZ, newLocation, matchItem.getZ(), 0, room).compose());
                                        }
                                    }
                                }
                            }

                            if (wired.shouldMatchAltitude() && !animatedAltitudeMove) {
                                if (matchItem.getZ() != setting.z) {
                                    matchItem.setZ(setting.z);
                                    forceUpdate = true;
                                }
                                if (forceUpdate) {
                                    matchItem.needsUpdate(true);
                                    room.updateItem(matchItem);
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    private double distanceToSavedTile(Room room, WiredMatchFurniSetting setting) {
        HabboItem item = room.getHabboItemByDatabaseId(setting.item_id);
        if (item == null) {
            return Double.MAX_VALUE;
        }

        double dx = item.getX() - setting.x;
        double dy = item.getY() - setting.y;
        return (dx * dx) + (dy * dy);
    }

    private double currentZ(Room room, WiredMatchFurniSetting setting) {
        HabboItem item = room.getHabboItemByDatabaseId(setting.item_id);
        return item != null ? item.getZ() : Double.MAX_VALUE;
    }
}
