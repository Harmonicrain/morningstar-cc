package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectProgressAchievement extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.PROGRESS_ACHIEVEMENT;

    public WiredEffectProgressAchievement(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectProgressAchievement(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override public boolean requiresTriggeringUser() { return true; }
    @Override public void execute(WiredContext ctx) { /* achievement binding is hotel-specific; open/save/persist supported. */ }
}
