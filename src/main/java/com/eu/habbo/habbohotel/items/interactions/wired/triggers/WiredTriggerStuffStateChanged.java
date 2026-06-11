package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.core.WiredEvent;
import com.eu.habbo.habbohotel.wired.core.WiredManager;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wired 2.0 trigger 20 (wf_trg_stuff_state): fires when a selected furni's
 * state changes. Unlike the legacy code-4 trigger this also fires for state
 * changes caused by wired effects, matching May behavior.
 */
public class WiredTriggerStuffStateChanged extends InteractionWiredTrigger {
    private static final WiredTriggerType type = WiredTriggerType.STUFF_STATE;

    private THashSet<HabboItem> items;

    /**
     * May intParams[0]: state-trigger mode radio. Persisted for round-trip;
     * fine-grained semantics (specific captured state vs any change) need an
     * official capture and currently both fire on any state change.
     */
    private int stateMode = 1;

    public WiredTriggerStuffStateChanged(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredTriggerStuffStateChanged(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean matches(HabboItem triggerItem, WiredEvent event) {
        if (event.getType() != WiredEvent.Type.FURNI_STATE_CHANGED) {
            return false;
        }
        HabboItem sourceItem = event.getSourceItem().orElse(null);
        return sourceItem != null && this.items.contains(sourceItem);
    }

    @Deprecated
    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredManager.getGson().toJson(new JsonData(
            this.stateMode,
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        String wiredData = set.getString("wired_data");

        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredManager.getGson().fromJson(wiredData, JsonData.class);
            this.stateMode = data.stateMode;
            for (Integer id : data.itemIds) {
                HabboItem item = room.getHabboItemByDatabaseId(id);
                if (item != null) {
                    this.items.add(item);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    // Wired 2.0 getters
    @Override
    protected int[] getWiredIntParams() { return new int[]{ this.stateMode }; }

    @Override
    protected java.util.Collection<HabboItem> getSelectedItems() { return this.items; }

    @Override
    protected boolean supportsFurniPicking() { return true; }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> stale = new THashSet<>();

        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItemByDatabaseId(item.getId()) == null) {
                stale.add(item);
            }
        }

        for (HabboItem item : stale) {
            this.items.remove(item);
        }

        message.appendBoolean(false);
        message.appendInt(WiredManager.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) {
            message.appendInt(item.getRoomVisibleId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getRoomVisibleId());
        message.appendString("");
        message.appendInt(1);
        message.appendInt(this.stateMode);
        message.appendInt(0);
        message.appendInt(type.code);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(WiredSettings settings) {
        this.stateMode = settings.getIntParams().length > 0 ? settings.getIntParams()[0] : 1;
        this.items.clear();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) {
            return false;
        }

        for (int furniId : settings.getFurniIds()) {
            HabboItem item = room.getHabboItem(furniId);
            if (item != null) {
                this.items.add(item);
            }
        }

        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return true;
    }

    static class JsonData {
        int stateMode;
        List<Integer> itemIds;

        public JsonData(int stateMode, List<Integer> itemIds) {
            this.stateMode = stateMode;
            this.itemIds = itemIds;
        }
    }
}
