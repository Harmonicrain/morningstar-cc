package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectMoveUserToFurni extends WiredEffectPhase3Base {
    public static final WiredEffectType type = WiredEffectType.MOVE_USER_TO_FURNI;

    public WiredEffectMoveUserToFurni(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredEffectMoveUserToFurni(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredEffectType getType() { return type; }
    @Override public boolean requiresTriggeringUser() { return true; }
    @Override protected boolean supportsFurniPickingWhenEmpty() { return true; }

    @Override
    public void execute(WiredContext ctx) {
        RoomUnit unit = ctx.actor().orElse(null);
        if (unit == null || this.items.isEmpty()) return;
        HabboItem target = this.items.get(0);
        RoomTile tile = ctx.room().getLayout().getTile(target.getX(), target.getY());
        if (tile != null) unit.setGoalLocation(tile);
    }
}
