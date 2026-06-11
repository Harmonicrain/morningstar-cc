package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectTeleportToRoom extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.TELEPORT_TO_ROOM;

    public WiredEffectTeleportToRoom(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectTeleportToRoom(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override public boolean requiresTriggeringUser() { return true; }
    @Override public void execute(WiredContext ctx) { /* room-change handoff is client/session work; persisted for Phase 3 UI. */ }
}
