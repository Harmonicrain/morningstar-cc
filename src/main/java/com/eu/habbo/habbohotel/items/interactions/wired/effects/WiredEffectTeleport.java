package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.pets.RideablePet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUnitType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.users.AvatarEffectMessageComposer;
import com.eu.habbo.messages.outgoing.rooms.users.UserUpdateMessageComposer;
import com.eu.habbo.threading.runnables.RoomUnitTeleport;
import com.eu.habbo.threading.runnables.SendRoomUnitEffectComposer;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectTeleport extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.TELEPORT;

    protected List<HabboItem> items;
    protected int[] furniSourceTypes = new int[0];
    protected int[] userSourceTypes = new int[0];

    public WiredEffectTeleport(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new ArrayList<>();
    }

    public WiredEffectTeleport(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new ArrayList<>();
    }

    public static void teleportUnitToTile(RoomUnit roomUnit, RoomTile tile) {
        if (roomUnit == null || tile == null || roomUnit.isWiredTeleporting)
            return;

        Room room = roomUnit.getRoom();

        if (room == null) {
            return;
        }

        // If this is a rider, sync the riding pet to the rider's current position immediately
        // Both will teleport together when the delay fires
        if (roomUnit.getRoomUnitType() == RoomUnitType.USER) {
            Habbo habbo = room.getHabbo(roomUnit);
            if (habbo != null && habbo.getHabboInfo() != null && habbo.getHabboInfo().getRiding() != null) {
                RideablePet ridingPet = habbo.getHabboInfo().getRiding();
                RoomUnit petUnit = ridingPet.getRoomUnit();
                if (petUnit != null) {
                    // Sync pet to rider's current position
                    RoomTile riderTile = roomUnit.getCurrentLocation();
                    petUnit.setLocation(riderTile);
                    petUnit.setZ(roomUnit.getZ() - 1.0);
                    petUnit.setPreviousLocation(riderTile);
                    petUnit.setGoalLocation(riderTile);
                    petUnit.removeStatus(RoomUnitStatus.MOVE);
                    petUnit.setCanWalk(false);
                    room.sendComposer(new UserUpdateMessageComposer(petUnit).compose());
                }
            }
        }

        // makes a temporary effect

        roomUnit.getRoom().unIdle(roomUnit.getRoom().getHabbo(roomUnit));
        room.sendComposer(new AvatarEffectMessageComposer(roomUnit, 4).compose());
        Emulator.getThreading().run(new SendRoomUnitEffectComposer(room, roomUnit), WiredManager.TELEPORT_DELAY + 1000);

        if (tile == roomUnit.getCurrentLocation()) {
            return;
        }

        if (tile.state == RoomTileState.INVALID || tile.state == RoomTileState.BLOCKED) {
            RoomTile alternativeTile = null;
            List<RoomTile> optionalTiles = room.getLayout().getTilesAround(tile);

            Collections.reverse(optionalTiles);
            for (RoomTile optionalTile : optionalTiles) {
                if (optionalTile.state != RoomTileState.INVALID && optionalTile.state != RoomTileState.BLOCKED) {
                    alternativeTile = optionalTile;
                    break;
                }
            }

            if (alternativeTile != null) {
                tile = alternativeTile;
            }
        }

        Emulator.getThreading().run(() -> { roomUnit.isWiredTeleporting = true; }, Math.max(0, WiredManager.TELEPORT_DELAY - 500));
        Emulator.getThreading().run(new RoomUnitTeleport(roomUnit, room, tile.x, tile.y, tile.getStackHeight() + (tile.state == RoomTileState.SIT ? -0.5 : 0), roomUnit.getEffectId()), WiredManager.TELEPORT_DELAY);
    }

    // Wired 2.0 getters
    @Override
    protected java.util.Collection<HabboItem> getSelectedItems() { return this.items; }
    @Override
    protected boolean supportsFurniPicking() { return true; }
    @Override
    protected boolean supportsUserPicking() { return true; }
    @Override
    protected boolean isWiredAdvancedMode() { return true; }
    @Override
    protected int[] getWiredFurniSourceTypes() { return this.furniSourceTypes; }
    @Override
    protected int[] getWiredUserSourceTypes() { return this.userSourceTypes; }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.items.removeIf(item -> item == null || item.getRoomId() != this.getRoomId()
                || Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItemByDatabaseId(item.getId()) == null);
        this.serializeWiredDataNew(message, room);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        int itemsCount = settings.getFurniIds().length;

        if(itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        List<HabboItem> newItems = new ArrayList<>();

        for (int i = 0; i < itemsCount; i++) {
            int itemId = settings.getFurniIds()[i];
            HabboItem it = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(itemId);

            if(it == null)
                throw new WiredSaveException(String.format("Item %s not found", itemId));

            newItems.add(it);
        }

        int delay = settings.getDelay();

        if(delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        this.items.clear();
        this.items.addAll(newItems);
        this.furniSourceTypes = settings.getFurniSourceTypes() != null ? settings.getFurniSourceTypes() : new int[0];
        this.userSourceTypes = settings.getUserSourceTypes() != null ? settings.getUserSourceTypes() : new int[0];
        this.setDelay(delay);

        return true;
    }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        if (room == null || room.getLayout() == null) {
            return;
        }
        
        this.items.removeIf(item -> item == null || item.getRoomId() != this.getRoomId()
                || Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItemByDatabaseId(item.getId()) == null);

        List<HabboItem> sourceItems = new ArrayList<>(resolveFurniSource(ctx, this.furniSourceTypes, 0, this.items, null));
        if (!sourceItems.isEmpty()) {
            int i = Emulator.getRandom().nextInt(sourceItems.size());
            HabboItem item = sourceItems.get(i);
            
            if (item == null) return;

            RoomTile tile = room.getLayout().getTile(item.getX(), item.getY());
            if (tile != null) {
                for (RoomUnit roomUnit : resolveUserSource(ctx, this.userSourceTypes, 0)) {
                    teleportUnitToTile(roomUnit, tile);
                }
            }
        }
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(
            this.getDelay(),
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList()),
            this.furniSourceTypes,
            this.userSourceTypes
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new ArrayList<>();
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.setDelay(data.delay);
            this.furniSourceTypes = data.furniSourceTypes != null ? data.furniSourceTypes : new int[0];
            this.userSourceTypes = data.userSourceTypes != null ? data.userSourceTypes : new int[0];
            for (Integer id: data.itemIds) {
                HabboItem item = room.getHabboItemByDatabaseId(id);
                if (item != null) {
                    this.items.add(item);
                }
            }
        } else {
            String[] wiredDataOld = wiredData.split("\t");

            if (wiredDataOld.length >= 1) {
                this.setDelay(Integer.parseInt(wiredDataOld[0]));
            }
            if (wiredDataOld.length == 2) {
                if (wiredDataOld[1].contains(";")) {
                    for (String s : wiredDataOld[1].split(";")) {
                        HabboItem item = room.getHabboItemByDatabaseId(Integer.parseInt(s));

                        if (item != null)
                            this.items.add(item);
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.furniSourceTypes = new int[0];
        this.userSourceTypes = new int[0];
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return true;
    }

    @Override
    protected long requiredCooldown() {
        return COOLDOWN_DEFAULT;
    }

    static class JsonData {
        int delay;
        List<Integer> itemIds;
        int[] furniSourceTypes;
        int[] userSourceTypes;

        public JsonData(int delay, List<Integer> itemIds, int[] furniSourceTypes, int[] userSourceTypes) {
            this.delay = delay;
            this.itemIds = itemIds;
            this.furniSourceTypes = furniSourceTypes;
            this.userSourceTypes = userSourceTypes;
        }
    }
}
