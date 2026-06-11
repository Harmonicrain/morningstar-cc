package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

abstract class WiredEffectPhase3Base extends InteractionWiredEffect {
    protected final List<HabboItem> items = new ArrayList<>();
    protected final List<HabboItem> items2 = new ArrayList<>();
    protected int[] intParams = new int[0];
    protected String stringParam = "";

    protected WiredEffectPhase3Base(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    protected WiredEffectPhase3Base(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.serializeWiredDataNew(message, room);
    }

    @Override
    public boolean saveData(WiredSettings settings, GameClient gameClient) throws WiredSaveException {
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) {
            return false;
        }

        this.intParams = settings.getIntParams() != null ? settings.getIntParams() : new int[0];
        this.stringParam = settings.getStringParam() != null ? settings.getStringParam() : "";
        this.setDelay(settings.getDelay());
        loadItems(room, this.items, settings.getFurniIds());
        loadItems(room, this.items2, settings.getFurniIds2());
        return true;
    }

    private void loadItems(Room room, List<HabboItem> target, int[] visibleIds) {
        target.clear();
        if (visibleIds == null) {
            return;
        }
        for (int visibleId : visibleIds) {
            HabboItem item = room.getHabboItem(visibleId);
            if (item != null) {
                target.add(item);
            }
        }
    }

    @Override
    public String getWiredData() {
        validateItems(this.items);
        validateItems(this.items2);
        return WiredManager.getGson().toJson(new JsonData(
                this.getDelay(),
                this.intParams,
                this.stringParam,
                this.items.stream().map(HabboItem::getId).collect(Collectors.toList()),
                this.items2.stream().map(HabboItem::getId).collect(Collectors.toList())));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        this.items2.clear();
        this.intParams = new int[0];
        this.stringParam = "";
        String wiredData = set.getString("wired_data");
        if (wiredData == null || !wiredData.startsWith("{")) {
            return;
        }
        JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
        this.setDelay(data.delay);
        this.intParams = data.intParams != null ? data.intParams : new int[0];
        this.stringParam = data.stringParam != null ? data.stringParam : "";
        if (data.itemIds != null) {
            for (Integer id : data.itemIds) {
                HabboItem item = room.getHabboItemByDatabaseId(id);
                if (item != null) {
                    this.items.add(item);
                }
            }
        }
        if (data.itemIds2 != null) {
            for (Integer id : data.itemIds2) {
                HabboItem item = room.getHabboItemByDatabaseId(id);
                if (item != null) {
                    this.items2.add(item);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.items2.clear();
        this.intParams = new int[0];
        this.stringParam = "";
        this.setDelay(0);
    }

    @Override
    protected Collection<HabboItem> getSelectedItems() {
        return this.items;
    }

    @Override
    protected Collection<HabboItem> getSelectedItems2() {
        return this.items2;
    }

    @Override
    protected int[] getWiredIntParams() {
        return this.intParams;
    }

    @Override
    protected String getWiredStringParam() {
        return this.stringParam;
    }

    @Override
    protected boolean supportsFurniPicking() {
        return !this.items.isEmpty() || supportsFurniPickingWhenEmpty();
    }

    protected boolean supportsFurniPickingWhenEmpty() {
        return false;
    }

    static class JsonData {
        int delay;
        int[] intParams;
        String stringParam;
        List<Integer> itemIds;
        List<Integer> itemIds2;

        JsonData(int delay, int[] intParams, String stringParam, List<Integer> itemIds, List<Integer> itemIds2) {
            this.delay = delay;
            this.intParams = intParams;
            this.stringParam = stringParam;
            this.itemIds = itemIds;
            this.itemIds2 = itemIds2;
        }
    }
}
