package com.eu.habbo.habbohotel.wired.variables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredVariable;
import com.eu.habbo.habbohotel.items.interactions.wired.variables.WiredVariableEcho;
import com.eu.habbo.habbohotel.items.interactions.wired.variables.WiredVariableReference;
import com.eu.habbo.habbohotel.rooms.Room;

import java.util.HashSet;
import java.util.Set;

/**
 * Bounded operational view of a Reference/Echo chain. Callers never receive a
 * writable target through a read-only edge, and cyclic durable edges fail
 * closed without resetting the visited set.
 */
public final class WiredVariableAliasOperations {
    private WiredVariableAliasOperations() {
    }

    public static Resolved resolve(Room room, String variableId) {
        Resolved active = fromActive(
                WiredVariableAliasResolver.resolve(room, variableId), false);
        return active != null
                ? active
                : resolveDurable(room, variableId, new HashSet<>(), 0, false);
    }

    public static boolean isAlias(Room room, String variableId) {
        if (room == null || variableId == null) {
            return false;
        }
        Boolean activeAlias = activeAlias(room, variableId);
        if (activeAlias != null) {
            return activeAlias;
        }
        WiredCrossRoomAliasRepository.Entry entry =
                WiredCrossRoomAliasRepository.instance()
                        .loadByAlias(room.getId(), variableId);
        return entry != null && entry.sourceRoomId() != null;
    }

    private static Resolved resolveDurable(
            Room room,
            String variableId,
            Set<String> visited,
            int depth,
            boolean readOnly) {
        if (room == null || variableId == null
                || depth >= WiredVariableAliasResolver.MAX_DEPTH
                || !visited.add(key(room.getId(), variableId))) {
            return null;
        }

        WiredCrossRoomAliasRepository.Entry alias =
                WiredCrossRoomAliasRepository.instance()
                        .loadByAlias(room.getId(), variableId);
        if (alias != null && alias.sourceRoomId() != null) {
            var environment = Emulator.getGameEnvironment();
            if (environment == null) {
                return null;
            }
            Room sourceRoom = environment.getRoomManager().getRoom(alias.sourceRoomId());
            if (sourceRoom == null) {
                return null;
            }
            return resolveDurable(
                    sourceRoom,
                    alias.sourceVariableId(),
                    visited,
                    depth + 1,
                    readOnly || alias.readOnly());
        }

        WiredVariableAliasResolver.Resolved active =
                WiredVariableAliasResolver.resolve(room, variableId);
        return fromActive(active, readOnly);
    }

    public static Integer read(Room room, String variableId, WiredVariableHolder holder) {
        WiredVariableValue value = readValue(room, variableId, holder);
        return value == null ? null : value.value();
    }

    public static WiredVariableValue readValue(
            Room room, String variableId, WiredVariableHolder holder) {
        if (room == null || holder == null) {
            return null;
        }
        Resolved active = resolve(room, variableId);
        if (active != null) {
            return acceptsHolder(room.getId(), active, holder)
                    ? active.manager().get(active.variableId(), holder)
                    : null;
        }
        return readUnloaded(
                room.getId(), room.getId(), variableId, holder,
                new HashSet<>(), 0);
    }

    private static WiredVariableValue readUnloaded(
            int originRoomId,
            int roomId,
            String variableId,
            WiredVariableHolder holder,
            Set<String> visited,
            int depth) {
        if (roomId <= 0 || variableId == null
                || depth >= WiredVariableAliasResolver.MAX_DEPTH
                || !visited.add(key(roomId, variableId))) {
            return null;
        }
        WiredCrossRoomAliasRepository repository =
                WiredCrossRoomAliasRepository.instance();
        WiredCrossRoomAliasRepository.Entry entry =
                repository.loadByAlias(roomId, variableId);
        if (entry == null) {
            return null;
        }
        if (entry.sourceRoomId() != null) {
            return readUnloaded(
                    originRoomId,
                    entry.sourceRoomId(),
                    entry.sourceVariableId(),
                    holder,
                    visited,
                    depth + 1);
        }

        if (holder.scope() == WiredVariableHolder.Scope.FURNI
                && roomId != originRoomId) {
            return null;
        }
        WiredCrossRoomAliasRepository.PersistedValue value =
                repository.loadPersistedValue(roomId, variableId, holder);
        if (value == null || value.scope() != holder.scope().code) {
            return null;
        }
        return new WiredVariableValue(
                variableId,
                holder,
                value.value(),
                value.createdAt(),
                value.updatedAt(),
                value.revision());
    }

    public static MutationTarget writable(
            Room room, String variableId, WiredVariableHolder holder) {
        Resolved resolved = resolve(room, variableId);
        return resolved == null || resolved.readOnly()
                || !acceptsHolder(room == null ? 0 : room.getId(), resolved, holder)
                ? null
                : new MutationTarget(
                        resolved.manager(), resolved.variableId(), holder);
    }

    public static WiredVariableMutation transformUnloaded(
            Room room,
            String variableId,
            WiredVariableHolder holder,
            WiredCrossRoomAliasRepository.ValueTransform transform) {
        return room == null ? null
                : WiredCrossRoomAliasRepository.instance().transformAliasValue(
                room.getId(), variableId, holder, transform);
    }

    public static WiredVariableMutation setUnloaded(
            Room room,
            String variableId,
            WiredVariableHolder holder,
            int value,
            boolean onlyIfAbsent) {
        return room == null ? null
                : WiredCrossRoomAliasRepository.instance().setAliasValue(
                room.getId(), variableId, holder, value, onlyIfAbsent);
    }

    public static WiredVariableMutation removeUnloaded(
            Room room, String variableId, WiredVariableHolder holder) {
        return room == null ? null
                : WiredCrossRoomAliasRepository.instance().removeAliasValue(
                room.getId(), variableId, holder);
    }

    private static boolean acceptsHolder(
            int originRoomId, Resolved resolved, WiredVariableHolder holder) {
        return resolved != null
                && resolved.definition().accepts(holder)
                && (holder.scope() != WiredVariableHolder.Scope.FURNI
                || resolved.manager().roomId() == originRoomId);
    }

    private static String key(int roomId, String variableId) {
        return roomId + ":" + variableId;
    }

    private static Resolved fromActive(
            WiredVariableAliasResolver.Resolved active, boolean readOnly) {
        if (active == null) {
            return null;
        }
        WiredVariableManager manager =
                active.room().getRoomSpecialTypes().getWiredVariableManager();
        return manager == null
                ? null
                : new Resolved(
                        manager,
                        active.variableId(),
                        active.definition(),
                        readOnly || active.readOnly());
    }

    /**
     * Avoids a registry query for ordinary active-room variables.  A null
     * result means the item cannot be identified safely and retains the
     * durable lookup fallback.
     */
    private static Boolean activeAlias(Room room, String variableId) {
        if (!variableId.startsWith("room:")) {
            return null;
        }
        try {
            InteractionWiredVariable item =
                    room.getRoomSpecialTypes().getVariable(
                            Integer.parseInt(variableId.substring(5)));
            return item == null
                    ? null
                    : item instanceof WiredVariableReference
                    || item instanceof WiredVariableEcho;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record Resolved(
            WiredVariableManager manager,
            String variableId,
            WiredVariableDefinition definition,
            boolean readOnly) {
    }

    public record MutationTarget(
            WiredVariableManager manager,
            String variableId,
            WiredVariableHolder holder) {
    }
}
