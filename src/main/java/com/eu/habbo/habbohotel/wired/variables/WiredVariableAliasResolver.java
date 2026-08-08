package com.eu.habbo.habbohotel.wired.variables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredVariable;
import com.eu.habbo.habbohotel.items.interactions.wired.variables.WiredVariableEcho;
import com.eu.habbo.habbohotel.items.interactions.wired.variables.WiredVariableReference;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.wired.WiredVariableAvailability;

import java.util.HashSet;
import java.util.Set;

/**
 * Resolves bounded Reference/Echo graphs among active rooms. Unavailable,
 * ambiguous or cyclic edges fail closed.
 *
 * <p>Reference/Echo behavior was adapted in part from Seth/iSetht's GPL-3.0
 * variable runtime:
 * https://github.com/Harmonicrain/Arcturus-Community-Wired. The graph storage
 * and cycle handling here are local, versioned implementations.</p>
 */
public final class WiredVariableAliasResolver {
    public static final int MAX_DEPTH = 16;

    private WiredVariableAliasResolver() {
    }

    public static Resolved resolve(Room room, String variableId) {
        return resolve(room, variableId, new HashSet<>(), 0);
    }

    /**
     * Resolves one shared-permanent definition by its globally stable variable
     * ID. Ambiguous active-room matches and alias cycles fail closed.
     */
    public static Resolved resolveShared(String variableId) {
        return findShared(variableId, new HashSet<>(), 0);
    }

    private static Resolved resolve(
            Room room, String variableId, Set<String> visited, int depth) {
        if (room == null || variableId == null || depth >= MAX_DEPTH
                || !visited.add(room.getId() + ":" + variableId)) {
            return null;
        }

        InteractionWiredVariable item = item(room, variableId);
        if (item instanceof WiredVariableEcho echo) {
            return resolve(room, echo.sourceId(), visited, depth + 1);
        }
        if (item instanceof WiredVariableReference reference) {
            Resolved source = findShared(
                    reference.sourceId(), visited, depth + 1);
            return source == null
                    ? null
                    : new Resolved(
                            source.room(),
                            source.variableId(),
                            source.definition(),
                            reference.readOnly() || source.readOnly());
        }

        WiredVariableManager manager =
                room.getRoomSpecialTypes().getWiredVariableManager();
        if (manager == null || !manager.isOperational()) {
            return null;
        }
        WiredVariableDefinition definition = manager.runtimeDefinition(variableId);
        return definition == null
                ? null
                : new Resolved(room, variableId, definition, false);
    }

    private static Resolved findShared(
            String variableId, Set<String> visited, int depth) {
        var environment = Emulator.getGameEnvironment();
        if (environment == null || variableId == null || depth >= MAX_DEPTH) {
            return null;
        }

        Resolved match = null;
        for (Room room : environment.getRoomManager().getActiveRooms()) {
            WiredVariableManager manager =
                    room.getRoomSpecialTypes().getWiredVariableManager();
            if (manager == null || !manager.isOperational()) {
                continue;
            }
            WiredVariableDefinition candidate =
                    manager.runtimeDefinition(variableId);
            if (candidate == null
                    || candidate.availabilityCode()
                    != WiredVariableAvailability.SHARED_PERMANENT.code) {
                continue;
            }
            // Variable IDs should be globally stable, but fail closed if corrupt
            // data exposes the same ID from more than one active room.
            if (match != null && match.room().getId() != room.getId()) {
                return null;
            }
            Resolved resolved = resolve(room, variableId, visited, depth);
            if (resolved == null) {
                return null;
            }
            match = resolved;
        }
        return match;
    }

    private static InteractionWiredVariable item(Room room, String variableId) {
        if (room == null || variableId == null || !variableId.startsWith("room:")) {
            return null;
        }
        try {
            return room.getRoomSpecialTypes().getVariable(
                    Integer.parseInt(variableId.substring(5)));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public record Resolved(
            Room room,
            String variableId,
            WiredVariableDefinition definition,
            boolean readOnly) {
    }
}
