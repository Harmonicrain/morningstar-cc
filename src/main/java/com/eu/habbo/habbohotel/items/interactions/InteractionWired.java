package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettings;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredSettingsNew;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredCategoryType;
import com.eu.habbo.habbohotel.wired.core.WiredManager;

import java.util.Collection;
import java.util.Collections;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.rooms.items.OneWayDoorStatusMessageComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base abstract class for all wired furniture items (triggers, effects, conditions, extras).
 * <p>
 * The wired system allows room owners to create automated behaviors in their rooms.
 * It consists of:
 * <ul>
 *   <li><b>Triggers</b> ({@link InteractionWiredTrigger}) - Events that start the wired chain</li>
 *   <li><b>Conditions</b> ({@link InteractionWiredCondition}) - Requirements that must be met</li>
 *   <li><b>Effects</b> ({@link InteractionWiredEffect}) - Actions that are executed</li>
 *   <li><b>Extras</b> ({@link InteractionWiredExtra}) - Modifiers like random selection</li>
 * </ul>
 * </p>
 * <p>
 * Wired items at the same tile coordinates form a "stack" and execute together.
 * The {@link com.eu.habbo.habbohotel.wired.core.WiredManager} orchestrates execution.
 * </p>
 * <p>
 * Key features:
 * <ul>
 *   <li>Cooldown system to prevent spam triggering</li>
 *   <li>Per-user execution caching with automatic cleanup</li>
 *   <li>JSON-based data persistence via {@link #getWiredData()}</li>
 * </ul>
 * </p>
 * 
 * @see com.eu.habbo.habbohotel.wired.core.WiredManager
 * @see com.eu.habbo.habbohotel.rooms.RoomSpecialTypes
 */
public abstract class InteractionWired extends InteractionDefault {
    private static final Logger LOGGER = LoggerFactory.getLogger(InteractionWired.class);
    
    /**
     * Maximum number of entries in the user execution cache to prevent memory leaks.
     */
    private static final int MAX_USER_CACHE_SIZE = 500;
    
    /**
     * Cache entries older than this (in milliseconds) will be cleaned up.
     * Default: 5 minutes
     */
    private static final long CACHE_EXPIRY_MS = 5 * 60 * 1000;
    
    private long cooldown;
    private final ConcurrentHashMap<Long, Long> userExecutionCache = new ConcurrentHashMap<>();

    InteractionWired(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.setExtradata("0");
    }

    InteractionWired(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.setExtradata("0");
    }

    /**
     * Executes this wired item's logic.
     * 
     * @param roomUnit the room unit that triggered this (may be null for non-user triggers)
     * @param room the room where this is happening
     * @param stuff additional context data passed from the trigger
     * @return true if execution was successful, false otherwise
     */
    public abstract boolean execute(RoomUnit roomUnit, Room room, Object[] stuff);

    public abstract String getWiredData();

    public abstract void serializeWiredData(ServerMessage message, Room room);

    public abstract void loadWiredData(ResultSet set, Room room) throws SQLException;

    @Override
    public void run() {
        if (this.needsUpdate()) {
            String wiredData = this.getWiredData();

            if (wiredData == null) {
                wiredData = "";
            }

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection(); PreparedStatement statement = connection.prepareStatement("UPDATE items SET wired_data = ? WHERE id = ?")) {
                if (this.getRoomId() != 0) {
                    statement.setString(1, wiredData);
                } else {
                    statement.setString(1, "");
                }
                statement.setInt(2, this.getId());
                statement.execute();
            } catch (SQLException e) {
                LOGGER.error("Caught SQL exception", e);
            }
        }
        super.run();
    }

    @Override
    public void onPickUp(Room room) {
        this.onPickUp();
    }

    public abstract void onPickUp();

    public void activateBox(Room room) {
        this.activateBox(room, (RoomUnit)null, 0L);
    }

    public void activateBox(Room room, RoomUnit roomUnit, long millis) {
        if(!room.isHideWired()) {
            this.setExtradata(this.getExtradata().equals("1") ? "0" : "1");
            room.sendComposer(new OneWayDoorStatusMessageComposer(this).compose());
        }
        if (roomUnit != null) {
            this.addUserExecutionCache(roomUnit.getId(), millis);
        }
    }

    protected long requiredCooldown() {
        return 50L;
    }


    public boolean canExecute(long newMillis) {
        return newMillis - this.cooldown >= this.requiredCooldown();
    }

    public void setCooldown(long newMillis) {
        this.cooldown = newMillis;
    }

    @Override
    public boolean allowWiredResetState() {
        return false;
    }

    @Override
    public boolean isUsable() {
        return true;
    }

    public boolean userCanExecute(int roomUnitId, long timestamp) {
        if (roomUnitId == -1) {
            return true;
        } else {
            if (this.userExecutionCache.containsKey((long)roomUnitId)) {
                long lastTimestamp = this.userExecutionCache.get((long)roomUnitId);
                return timestamp - lastTimestamp >= Math.max(100L, this.requiredCooldown());
            }

            return true;
        }
    }

    public void clearUserExecutionCache() {
        this.userExecutionCache.clear();
    }

    public void addUserExecutionCache(int roomUnitId, long timestamp) {
        // Enforce max size limit to prevent memory leaks
        if (this.userExecutionCache.size() >= MAX_USER_CACHE_SIZE) {
            cleanExpiredCacheEntries(timestamp);
            
            // If still too large after cleanup, remove oldest entries
            if (this.userExecutionCache.size() >= MAX_USER_CACHE_SIZE) {
                // Remove approximately 10% of entries
                int toRemove = MAX_USER_CACHE_SIZE / 10;
                Iterator<Map.Entry<Long, Long>> iterator = this.userExecutionCache.entrySet().iterator();
                while (iterator.hasNext() && toRemove > 0) {
                    iterator.next();
                    iterator.remove();
                    toRemove--;
                }
            }
        }
        this.userExecutionCache.put((long) roomUnitId, timestamp);
    }
    
    /**
     * Removes cache entries older than CACHE_EXPIRY_MS.
     * @param currentTimestamp the current timestamp to compare against
     */
    public void cleanExpiredCacheEntries(long currentTimestamp) {
        this.userExecutionCache.entrySet().removeIf(
            entry -> currentTimestamp - entry.getValue() > CACHE_EXPIRY_MS
        );
    }
    
    /**
     * Gets the current size of the user execution cache.
     * @return the number of cached entries
     */
    public int getUserExecutionCacheSize() {
        return this.userExecutionCache.size();
    }

    // ===== Wired 2.0 serialization (new packet format). ADDITIVE =====
    // Not yet invoked by the data composers; the switch-over (compose via this
    // method + drop the legacy serializeWiredData overrides) is a follow-up slice.
    // Subclasses override only the getters for the data they carry.

    /** Category drives the type-specific blocks. Mid-level bases override. */
    protected WiredCategoryType getWiredCategory() { return WiredCategoryType.TRIGGER; }

    /** Type code sent in the new packet. Mid-level bases return getType().code. */
    protected int getWiredTypeCode() { return 0; }

    protected int getMaxFurniSelection() { return WiredManager.MAXIMUM_FURNI_SELECTION; }
    protected Collection<HabboItem> getSelectedItems() { return Collections.emptyList(); }
    protected Collection<HabboItem> getSelectedItems2() { return Collections.emptyList(); }
    protected String getWiredStringParam() { return ""; }
    protected int[] getWiredIntParams() { return new int[0]; }
    protected String[] getWiredVariableIds() { return new String[0]; }
    protected int[] getWiredFurniSourceTypes() { return new int[0]; }
    protected int[] getWiredUserSourceTypes() { return new int[0]; }
    protected int getWiredDelay() { return 0; }
    protected int getWiredQuantifierCode() { return 0; }
    protected byte getWiredQuantifierType() { return 0; }
    protected boolean isWiredInvert() { return false; }
    protected boolean isWiredFilter() { return false; }
    protected boolean isWiredAdvancedMode() { return false; }
    protected boolean isWiredAllowWallFurni() { return false; }
    protected boolean supportsFurniPicking() { return false; }
    protected boolean supportsUserPicking() { return false; }

    /**
     * Writes the Wired 2.0 data payload (see wired-port-plan.md "New Server
     * Serialization Order"). WiredContext is always 0 blocks and defaultIntParams
     * always empty until Phase 6.
     */
    public void serializeWiredDataNew(ServerMessage message, Room room) {
        WiredCategoryType category = getWiredCategory();

        message.appendInt(getMaxFurniSelection());            // furniLimit
        appendVisibleIds(message, getSelectedItemVisibleIds());  // stuffIds
        appendVisibleIds(message, getSelectedItem2VisibleIds()); // stuffIds2
        message.appendInt(this.getBaseItem().getSpriteId());  // stuffTypeId
        message.appendInt(this.getRoomVisibleId());           // id
        message.appendString(getWiredStringParam());          // stringParam
        appendInts(message, getWiredIntParams());             // intParams
        appendStrings(message, getWiredVariableIds());        // variableIds
        appendInts(message, getWiredFurniSourceTypes());      // furniSourceTypes
        appendInts(message, getWiredUserSourceTypes());       // userSourceTypes
        message.appendInt(getWiredTypeCode());                // code

        // readDefinitionSpecifics
        switch (category) {
            case EFFECT:    message.appendInt(getWiredDelay()); break;
            case CONDITION: message.appendInt(getWiredQuantifierCode()); break;
            case SELECTOR:  message.appendBoolean(isWiredFilter()); message.appendBoolean(isWiredInvert()); break;
            default: break; // TRIGGER, ADDON, VARIABLE
        }

        message.appendBoolean(isWiredAdvancedMode());         // advancedMode
        serializeInputSourcesConf(message);                   // InputSourcesConf
        message.appendBoolean(isWiredAllowWallFurni());       // allowWallFurni

        // readTypeSpecifics
        if (category == WiredCategoryType.CONDITION) {
            message.appendByte((int) getWiredQuantifierType());
            message.appendBoolean(isWiredInvert());
        }

        message.appendInt(0);                                 // WiredContext block count (0 until Phase 6)
        message.appendInt(0);                                 // defaultIntParams count
    }

    /** Room-visible ids of the primary furni selection. Custom classes (settings-
     *  backed items with id fallback) override this directly. */
    protected int[] getSelectedItemVisibleIds() { return toVisibleIds(getSelectedItems()); }

    /** Room-visible ids of the secondary furni selection (stuffIds2). */
    protected int[] getSelectedItem2VisibleIds() { return toVisibleIds(getSelectedItems2()); }

    private int[] toVisibleIds(Collection<HabboItem> items) {
        int[] ids = new int[items.size()];
        int i = 0;
        for (HabboItem item : items) {
            ids[i++] = item.getRoomVisibleId();
        }
        return ids;
    }

    private void appendVisibleIds(ServerMessage message, int[] ids) {
        message.appendInt(ids.length);
        for (int id : ids) {
            message.appendInt(id);
        }
    }

    private void appendInts(ServerMessage message, int[] values) {
        message.appendInt(values.length);
        for (int v : values) {
            message.appendInt(v);
        }
    }

    private void appendStrings(ServerMessage message, String[] values) {
        message.appendInt(values.length);
        for (String s : values) {
            message.appendString(s);
        }
    }

    /** Default InputSourcesConf: enables the furni picker (constant 100) when the type supports it. */
    private void serializeInputSourcesConf(ServerMessage message) {
        if (supportsFurniPicking()) {
            message.appendInt(1);   // allowedFurniSources outer count
            message.appendInt(1);   // slot 0 inner count
            message.appendInt(100); // FURNI_SOURCE_FURNI_PICKS_1
        } else {
            message.appendInt(0);
        }
        message.appendInt(0);       // allowedUserSources (none in Phase 1)
        if (supportsFurniPicking()) {
            message.appendInt(1);   // defaultFurniSources
            message.appendInt(100);
        } else {
            message.appendInt(0);
        }
        message.appendInt(0);       // defaultUserSources (none in Phase 1)
    }

    public static WiredSettings readSettings(ClientMessage packet, boolean isEffect)
    {
        int intParamCount = packet.readInt();
        int[] intParams = new int[intParamCount];

        for(int i = 0; i < intParamCount; i++)
        {
            intParams[i] = packet.readInt();
        }

        String stringParam = packet.readString();

        int itemCount = packet.readInt();
        int[] itemIds = new int[itemCount];

        for(int i = 0; i < itemCount; i++)
        {
            itemIds[i] = packet.readInt();
        }

        WiredSettings settings = new WiredSettings(intParams, stringParam, itemIds, -1);

        if(isEffect)
        {
            settings.setDelay(packet.readInt());
        }

        settings.setStuffTypeSelectionCode(packet.readInt());
        return settings;
    }

    /**
     * Wired 2.0 save reader — matches the new client composer write order
     * (see wired-port-plan.md "New Server Read Order"). Does NOT read the legacy
     * stuffTypeSelectionCode (removed from 2.0). Type-specific block is driven by
     * {@code category}: EFFECT->delay, CONDITION->quantifierCode,
     * SELECTOR->isFilter+isInvert; others have no type-specific field.
     */
    public static WiredSettingsNew readSettingsNew(ClientMessage packet, WiredCategoryType category)
    {
        // Common prefix
        int[] intParams = readCountedInts(packet);
        String stringParam = packet.readString();
        int[] furniIds = readCountedInts(packet);

        // Type-specific block
        int delay = 0;
        int quantifierCode = 0;
        boolean isFilter = false;
        boolean isInvert = false;
        switch (category) {
            case EFFECT:
                delay = packet.readInt();
                break;
            case CONDITION:
                quantifierCode = packet.readInt();
                break;
            case SELECTOR:
                isFilter = packet.readBoolean();
                isInvert = packet.readBoolean();
                break;
            default:
                break; // TRIGGER, ADDON, VARIABLE: no type-specific field
        }

        // Common suffix
        int[] furniSourceTypes = readCountedInts(packet);
        int[] userSourceTypes = readCountedInts(packet);
        String[] variableIds = readCountedStrings(packet);
        int[] furniIds2 = readCountedInts(packet);

        return new WiredSettingsNew(intParams, stringParam, furniIds, furniIds2, variableIds,
                furniSourceTypes, userSourceTypes, delay, quantifierCode, isFilter, isInvert);
    }

    private static int[] readCountedInts(ClientMessage packet) {
        int count = packet.readInt();
        int[] out = new int[Math.max(count, 0)];
        for (int i = 0; i < count; i++) {
            out[i] = packet.readInt();
        }
        return out;
    }

    private static String[] readCountedStrings(ClientMessage packet) {
        int count = packet.readInt();
        String[] out = new String[Math.max(count, 0)];
        for (int i = 0; i < count; i++) {
            out[i] = packet.readString();
        }
        return out;
    }
}
