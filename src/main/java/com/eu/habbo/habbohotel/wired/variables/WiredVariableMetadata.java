package com.eu.habbo.habbohotel.wired.variables;

import com.eu.habbo.habbohotel.items.interactions.wired.addons.WiredAddonVariablePlaceholder;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.WiredVariableType;
import com.eu.habbo.messages.ServerMessage;

import java.util.Map;

/** Serializes one July WiredVariable metadata record. */
public final class WiredVariableMetadata {
    public static final int STORED_VARIABLE_TYPE = 0;
    public static final int GLOBAL_TARGET = -10;

    private final String variableId;
    private final int variableType;
    private final String variableName;
    private final int availability;
    private final int variableTarget;
    private final boolean alwaysAvailable;
    private final boolean canCreateAndDelete;
    private final boolean hasValue;
    private final boolean canWriteValue;
    private final boolean canInterceptChanges;
    private final boolean invisible;
    private final boolean canReadCreationTime;
    private final boolean canReadLastUpdateTime;
    private final Map<Long, String> textConnectorValues;

    private WiredVariableMetadata(String variableId, int variableType, String variableName,
                                  int availability, int variableTarget,
                                  boolean alwaysAvailable, boolean canCreateAndDelete,
                                  boolean hasValue, boolean canWriteValue,
                                  boolean canInterceptChanges, boolean invisible,
                                  boolean canReadCreationTime, boolean canReadLastUpdateTime,
                                  Map<Long, String> textConnectorValues) {
        this.variableId = variableId;
        this.variableType = variableType;
        this.variableName = variableName;
        this.availability = availability;
        this.variableTarget = variableTarget;
        this.alwaysAvailable = alwaysAvailable;
        this.canCreateAndDelete = canCreateAndDelete;
        this.hasValue = hasValue;
        this.canWriteValue = canWriteValue;
        this.canInterceptChanges = canInterceptChanges;
        this.invisible = invisible;
        this.canReadCreationTime = canReadCreationTime;
        this.canReadLastUpdateTime = canReadLastUpdateTime;
        this.textConnectorValues = textConnectorValues == null ? Map.of() : Map.copyOf(textConnectorValues);
    }

    /**
     * Adapter for the Global/Room Variable singleton.
     *
     * <p>The July client proves the field order, target {@code -10}, and
     * availability values, but client code cannot prove the server-generated ID
     * or capability flags. These values intentionally emulate the donor's
     * lifecycle behind a stable item-based ID. Replace this one factory when an
     * official open-editor capture becomes available.</p>
     */
    public static WiredVariableMetadata roomGlobal(int databaseItemId, String name, int availability) {
        return stored("room:" + databaseItemId, name, availability, GLOBAL_TARGET, true, Map.of());
    }

    public static WiredVariableMetadata roomGlobal(Room room, int databaseItemId, String name, int availability) {
        String variableId = "room:" + databaseItemId;
        return stored(variableId, name, availability, GLOBAL_TARGET, true,
                WiredAddonVariablePlaceholder.textConverterValues(room, variableId));
    }

    /**
     * Adapter for a room-visible stored definition.  The opaque variable ID is
     * retained from the persisted definition; it must never be re-derived from
     * a room-visible furniture ID.
     *
     * <p>The July parser proves this record's field order and holder-target
     * domain.  Its capability flags remain the documented emulation until an
     * official editor capture establishes the exact flags per definition.</p>
     */
    public static WiredVariableMetadata fromDefinition(WiredVariableDefinition definition) {
        return fromDefinition(null, definition);
    }

    public static WiredVariableMetadata fromDefinition(Room room, WiredVariableDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("definition is required");
        }
        int target = switch (definition.type()) {
            case FURNI -> 0;
            case USER, QUEST, QUEST_CHAIN -> 1;
            case ROOM -> GLOBAL_TARGET;
            default -> throw new IllegalArgumentException(
                    "definition is not room-visible: " + definition.type());
        };
        if (definition.type() == WiredVariableType.QUEST
                || definition.type() == WiredVariableType.QUEST_CHAIN) {
            return readOnlyStored(definition.variableId(), definition.name(),
                    definition.availabilityCode(), target);
        }
        return stored(definition.variableId(), definition.name(), definition.availabilityCode(), target,
                definition.hasValue(), WiredAddonVariablePlaceholder.textConverterValues(room, definition.variableId()));
    }

    public static WiredVariableMetadata generated(String variableId, String variableName,
                                                   WiredVariableDefinition parent) {
        if (parent == null) throw new IllegalArgumentException("parent is required");
        int target = switch (parent.type()) {
            case FURNI -> 0;
            case USER -> 1;
            case ROOM -> GLOBAL_TARGET;
            default -> throw new IllegalArgumentException("generated parent is not room-visible");
        };
        return new WiredVariableMetadata(variableId, STORED_VARIABLE_TYPE, variableName,
                parent.availabilityCode(), target, true, false, true, false, false,
                false, false, false, Map.of());
    }

    /** July variable type 1: calculated engine value exposed in the Internal tab. */
    public static WiredVariableMetadata internal(
            WiredInternalVariableRuntime.Definition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("internal definition is required");
        }
        return new WiredVariableMetadata(
                definition.variableId(),
                1,
                definition.name(),
                WiredInternalVariableRuntime.AVAILABILITY_INTERNAL,
                definition.target(),
                definition.alwaysAvailable(),
                false,
                true,
                definition.writable(),
                false,
                false,
                false,
                false,
                Map.of());
    }

    /**
     * Stable local cache token for the exact metadata serialized to AIR.
     *
     * <p>July treats the per-variable hash as opaque.  Our emulated token must
     * nevertheless change whenever any serialized field changes; in
     * particular, placeholder add-ons can alter the text-connector map without
     * changing the underlying variable definition.</p>
     */
    int catalogHash() {
        int hash = 17;
        hash = 31 * hash + this.variableId.hashCode();
        hash = 31 * hash + this.variableType;
        hash = 31 * hash + this.variableName.hashCode();
        hash = 31 * hash + this.availability;
        hash = 31 * hash + this.variableTarget;
        hash = 31 * hash + Boolean.hashCode(this.alwaysAvailable);
        hash = 31 * hash + Boolean.hashCode(this.canCreateAndDelete);
        hash = 31 * hash + Boolean.hashCode(this.hasValue);
        hash = 31 * hash + Boolean.hashCode(this.canWriteValue);
        hash = 31 * hash + Boolean.hashCode(this.canInterceptChanges);
        hash = 31 * hash + Boolean.hashCode(this.invisible);
        hash = 31 * hash + Boolean.hashCode(this.canReadCreationTime);
        hash = 31 * hash + Boolean.hashCode(this.canReadLastUpdateTime);
        hash = 31 * hash + this.textConnectorValues.size();
        for (Map.Entry<Long, String> entry : this.textConnectorValues.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            hash = 31 * hash + Long.hashCode(entry.getKey());
            hash = 31 * hash + entry.getValue().hashCode();
        }
        return hash;
    }

    private static WiredVariableMetadata stored(String variableId, String name, int availability,
                                                int target) {
        return stored(variableId, name, availability, target, true, Map.of());
    }

    private static WiredVariableMetadata stored(String variableId, String name, int availability,
                                                int target, boolean hasValue,
                                                Map<Long, String> textConnectorValues) {
        return new WiredVariableMetadata(
                variableId,
                STORED_VARIABLE_TYPE,
                name == null ? "" : name,
                availability,
                target,
                true,
                hasValue,
                hasValue,
                true,
                true,
                false,
                true,
                true,
                textConnectorValues);
    }

    private static WiredVariableMetadata readOnlyStored(String variableId, String name,
                                                        int availability, int target) {
        return new WiredVariableMetadata(
                variableId,
                STORED_VARIABLE_TYPE,
                name == null ? "" : name,
                availability,
                target,
                true,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                Map.of());
    }

    /** Exact July WiredVariable field order, including the absent text-connector flag. */
    public void serialize(ServerMessage message) {
        message.appendString(this.variableId);
        message.appendInt(this.variableType);
        message.appendString(this.variableName);
        message.appendInt(this.availability);
        message.appendInt(this.variableTarget);
        message.appendBoolean(this.alwaysAvailable);
        message.appendBoolean(this.canCreateAndDelete);
        message.appendBoolean(this.hasValue);
        message.appendBoolean(this.canWriteValue);
        message.appendBoolean(this.canInterceptChanges);
        message.appendBoolean(this.invisible);
        message.appendBoolean(this.canReadCreationTime);
        message.appendBoolean(this.canReadLastUpdateTime);
        message.appendBoolean(!this.textConnectorValues.isEmpty());
        if (!this.textConnectorValues.isEmpty()) {
            message.appendInt(this.textConnectorValues.size());
            this.textConnectorValues.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        message.appendInt(entry.getKey().intValue());
                        message.appendString(entry.getValue());
                    });
        }
    }
}
