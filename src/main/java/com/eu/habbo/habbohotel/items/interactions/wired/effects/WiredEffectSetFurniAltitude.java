package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectSetFurniAltitude extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.SET_FURNI_ALTITUDE;

    public WiredEffectSetFurniAltitude(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectSetFurniAltitude(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override protected boolean supportsFurniPickingWhenEmpty() { return true; }

    @Override
    public void execute(WiredContext ctx) {
        Room room = ctx.room();
        int amount = this.intParams.length > 0 ? this.intParams[0] : 0;
        int operator = this.intParams.length > 1 ? this.intParams[1] : 0;
        for (HabboItem item : this.items) {
            RoomTile tile = room.getLayout().getTile(item.getX(), item.getY());
            if (tile == null) continue;
            double oldZ = item.getZ();
            double target = amount / 100.0;
            if (operator == 1) target = oldZ + target;
            if (operator == 2) target = oldZ - target;
            item.setZ(target);
            room.sendComposer(new FloorItemOnRollerComposer(item, null, tile, oldZ, tile, item.getZ(), 0, room).compose());
        }
    }
}
