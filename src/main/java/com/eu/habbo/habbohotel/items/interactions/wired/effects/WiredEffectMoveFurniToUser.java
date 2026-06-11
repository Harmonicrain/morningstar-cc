package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectMoveFurniToUser extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.MOVE_FURNI_TO_USER;

    public WiredEffectMoveFurniToUser(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectMoveFurniToUser(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override public boolean requiresTriggeringUser() { return true; }
    @Override protected boolean supportsFurniPickingWhenEmpty() { return true; }

    @Override
    public void execute(WiredContext ctx) {
        RoomUnit unit = ctx.actor().orElse(null);
        Room room = ctx.room();
        if (unit == null) return;
        RoomTile tile = unit.getCurrentLocation();
        if (tile == null) return;
        for (HabboItem item : this.items) {
            room.moveFurniTo(item, tile, item.getRotation(), null, true, false);
        }
    }
}
