package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectMoveFurniToFurni extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.MOVE_FURNI_TO_FURNI;

    public WiredEffectMoveFurniToFurni(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectMoveFurniToFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override protected boolean supportsFurniPickingWhenEmpty() { return true; }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        if (this.items2.isEmpty()) return;
        HabboItem target = this.items2.get(0);
        RoomTile tile = room.getLayout().getTile(target.getX(), target.getY());
        if (tile == null) return;
        for (HabboItem item : this.items) {
            if (item != target) {
                room.moveFurniTo(item, tile, item.getRotation(), null, true, false);
            }
        }
    }
}
