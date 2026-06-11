package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionPerformingAction extends WiredConditionPhase3Base {
    public static final WiredConditionType type = WiredConditionType.PERFORMING_ACTION;

    public WiredConditionPerformingAction(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredConditionPerformingAction(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredConditionType getType() { return type; }

    @Override
    public boolean evaluate(WiredContext ctx) {
        RoomUnit actor = ctx.actor().orElse(null);
        if (actor == null) {
            return false;
        }

        int action = this.intParams.length > 0 ? this.intParams[0] : 0;
        switch (action) {
            case 1:
                return actor.hasStatus(RoomUnitStatus.SIT) || actor.hasStatus(RoomUnitStatus.SIT_IN);
            case 2:
                return actor.hasStatus(RoomUnitStatus.LAY) || actor.hasStatus(RoomUnitStatus.LAY_IN);
            case 3:
                return actor.hasStatus(RoomUnitStatus.DANCE);
            case 0:
            default:
                return actor.hasStatus(RoomUnitStatus.MOVE) || actor.isWalking();
        }
    }
}
