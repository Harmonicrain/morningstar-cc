package com.eu.habbo.habbohotel.wired.variables;

import com.eu.habbo.habbohotel.rooms.Room;
import java.util.*;

/** Room catalog combining stored definitions with calculated July child variables. */
public final class WiredVariableCatalog {
    private WiredVariableCatalog() { }
    public static Catalog build(Room room, WiredVariableManager.Snapshot snapshot) {
        if (room == null || snapshot == null || !snapshot.available()) return new Catalog(0, Map.of());
        List<Entry> ordered = new ArrayList<>();
        for (WiredVariableManager.VariableSnapshot variable : snapshot.variables().values()) {
            WiredVariableMetadata metadata =
                    WiredVariableMetadata.fromDefinition(room, variable.definition());
            ordered.add(new Entry(
                    variable.definition().variableId(),
                    catalogHash(variable.hash(), metadata),
                    metadata));
            for (WiredGeneratedVariableRuntime.GeneratedDefinition child
                    : WiredGeneratedVariableRuntime.definitions(room, variable.definition())) {
                WiredVariableMetadata childMetadata = WiredVariableMetadata.generated(
                        child.variableId(), child.variableName(), child.parent());
                ordered.add(new Entry(
                        child.variableId(),
                        catalogHash(child.hash(), childMetadata),
                        childMetadata));
            }
        }
        for (WiredInternalVariableRuntime.Definition internal
                : WiredInternalVariableRuntime.definitions()) {
            WiredVariableMetadata metadata = WiredVariableMetadata.internal(internal);
            ordered.add(new Entry(
                    internal.variableId(),
                    catalogHash(internal.hash(), metadata),
                    metadata));
        }
        ordered.sort(Comparator.comparing(Entry::variableId));
        LinkedHashMap<String,Entry> entries = new LinkedHashMap<>();
        int aggregate = 1;
        for (Entry entry : ordered) {
            entries.put(entry.variableId(), entry);
            aggregate = 31 * aggregate + entry.variableId().hashCode();
            aggregate = 31 * aggregate + entry.hash();
        }
        return new Catalog(aggregate, entries);
    }
    private static int catalogHash(int sourceHash, WiredVariableMetadata metadata) {
        return 31 * sourceHash + metadata.catalogHash();
    }
    public record Entry(String variableId, int hash, WiredVariableMetadata metadata) { }
    public record Catalog(int aggregateHash, Map<String,Entry> entries) {
        public Catalog { entries = Collections.unmodifiableMap(new LinkedHashMap<>(entries)); }
    }
}
