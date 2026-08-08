package com.eu.habbo.habbohotel.games.gamehall.leaderboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public enum GamehallLeaderboardPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    ALLTIME;

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    public static GamehallLeaderboardPeriod fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return WEEKLY;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace("-", "").replace("_", "");
        switch (normalized) {
            case "DAY":
            case "DAILY":
            case "TODAY":
                return DAILY;
            case "WEEK":
            case "WEEKLY":
            case "THISWEEK":
                return WEEKLY;
            case "MONTH":
            case "MONTHLY":
            case "THISMONTH":
                return MONTHLY;
            case "ALL":
            case "ALLTIME":
                return ALLTIME;
            default:
                return WEEKLY;
        }
    }

    public long getCurrentPeriodStart() {
        LocalDate today = LocalDate.now(ZONE_ID);
        ZonedDateTime start;

        switch (this) {
            case DAILY:
                start = today.atStartOfDay(ZONE_ID);
                break;
            case WEEKLY:
                start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay(ZONE_ID);
                break;
            case MONTHLY:
                start = today.withDayOfMonth(1).atStartOfDay(ZONE_ID);
                break;
            case ALLTIME:
            default:
                return 0;
        }

        return start.toEpochSecond();
    }
}
