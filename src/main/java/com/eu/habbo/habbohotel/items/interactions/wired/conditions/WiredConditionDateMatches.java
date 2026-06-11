package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.core.WiredContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;

public class WiredConditionDateMatches extends WiredConditionPhase3Base {
    public static final WiredConditionType type = WiredConditionType.DATE_MATCHES;

    public WiredConditionDateMatches(ResultSet set, Item baseItem) throws SQLException { super(set, baseItem); }
    public WiredConditionDateMatches(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) { super(id, userId, item, extradata, limitedStack, limitedSells); }

    @Override public WiredConditionType getType() { return type; }

    @Override
    public boolean evaluate(WiredContext ctx) {
        Calendar now = Calendar.getInstance();
        int day = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH) + 1;
        int year = now.get(Calendar.YEAR);
        int weekday = now.get(Calendar.DAY_OF_WEEK);

        if (this.intParams.length > 0 && this.intParams[0] != 0) {
            int min = this.intParams.length > 3 ? this.intParams[3] : day;
            int max = this.intParams.length > 4 ? this.intParams[4] : day;
            if (day < min || day > max) {
                return false;
            }
        }

        if (this.intParams.length > 2 && this.intParams[2] != 0 && !maskContains(this.intParams[2], weekday)) {
            return false;
        }

        if (this.intParams.length > 5 && this.intParams[5] != 0 && !maskContains(this.intParams[5], month)) {
            return false;
        }

        if (this.intParams.length > 1 && this.intParams[1] != 0) {
            int min = this.intParams.length > 6 ? this.intParams[6] : year;
            int max = this.intParams.length > 7 ? this.intParams[7] : year;
            return year >= min && year <= max;
        }

        return true;
    }

    private boolean maskContains(int mask, int oneBasedValue) {
        return (mask & (1 << Math.max(0, oneBasedValue - 1))) != 0;
    }
}
