package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectPlaceFurni extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.PLACE_FURNI;

    public WiredEffectPlaceFurni(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectPlaceFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override public void execute(WiredContext ctx) { /* requires inventory/BC placement policy; persisted for Phase 3 UI. */ }
}
