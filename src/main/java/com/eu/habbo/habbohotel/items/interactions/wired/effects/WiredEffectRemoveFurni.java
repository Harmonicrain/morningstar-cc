package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectRemoveFurni extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.REMOVE_FURNI;

    public WiredEffectRemoveFurni(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectRemoveFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override protected boolean supportsFurniPickingWhenEmpty() { return true; }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        for (HabboItem item : sourceItems(ctx).toArray(new HabboItem[0])) {
            room.pickUpItem(item, Emulator.getGameEnvironment().getHabboManager().getHabbo(item.getUserId()));
        }
    }
}
