package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class WiredConditionTimeMatches extends WiredConditionPhase3Base {
    public static final WiredConditionType type = WiredConditionType.TIME_MATCHES;

    public WiredConditionTimeMatches(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredConditionTimeMatches(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredConditionType getType() { return type; }

    @Override
    public boolean evaluate(WiredContext ctx) {
        Calendar now = Calendar.getInstance();
        return inRange(this.intParams, 0, now.get(Calendar.SECOND), 3, 4)
                && inRange(this.intParams, 1, now.get(Calendar.MINUTE), 5, 6)
                && inRange(this.intParams, 2, now.get(Calendar.HOUR_OF_DAY), 7, 8);
    }

    private boolean inRange(int[] params, int enabledIndex, int value, int minIndex, int maxIndex) {
        if (params.length <= enabledIndex || params[enabledIndex] == 0) {
            return true;
        }
        int min = params.length > minIndex ? params[minIndex] : value;
        int max = params.length > maxIndex ? params[maxIndex] : value;
        return value >= min && value <= max;
    }
}
