package com.eu.habbo.habbohotel.games.gamehall.leaderboard;

import java.util.Collections;
import java.util.List;

public class GamehallLeaderboardData {
    private final String gameType;
    private final GamehallLeaderboardPeriod period;
    private final int offset;
    private final int limit;
    private final int totalRows;
    private final List<GamehallLeaderboardRow> rows;
    private final GamehallLeaderboardRow ownRow;

    public GamehallLeaderboardData(String gameType, GamehallLeaderboardPeriod period, int offset, int limit,
                                   int totalRows, List<GamehallLeaderboardRow> rows, GamehallLeaderboardRow ownRow) {
        this.gameType = gameType;
        this.period = period;
        this.offset = offset;
        this.limit = limit;
        this.totalRows = totalRows;
        this.rows = rows == null ? Collections.emptyList() : Collections.unmodifiableList(rows);
        this.ownRow = ownRow;
    }

    public String getGameType() {
        return this.gameType;
    }

    public GamehallLeaderboardPeriod getPeriod() {
        return this.period;
    }

    public int getOffset() {
        return this.offset;
    }

    public int getLimit() {
        return this.limit;
    }

    public int getTotalRows() {
        return this.totalRows;
    }

    public List<GamehallLeaderboardRow> getRows() {
        return this.rows;
    }

    public GamehallLeaderboardRow getOwnRow() {
        return this.ownRow;
    }
}
