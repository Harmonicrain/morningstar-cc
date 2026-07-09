package com.eu.habbo.messages.outgoing.games.gamehall;

import com.eu.habbo.habbohotel.games.gamehall.leaderboard.GamehallLeaderboardData;
import com.eu.habbo.habbohotel.games.gamehall.leaderboard.GamehallLeaderboardRow;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

/**
 * Opens the Games Hall leaderboard using server-owned ranking data.
 */
public class OpenGamehallLeaderboardMessageComposer extends MessageComposer {
    private final GamehallLeaderboardData data;

    public OpenGamehallLeaderboardMessageComposer(GamehallLeaderboardData data) {
        this.data = data;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.OpenGamehallLeaderboardMessageComposer);
        this.response.appendString(this.data != null ? this.data.getGameType() : "ALL");
        this.response.appendString(this.data != null && this.data.getPeriod() != null ? this.data.getPeriod().name() : "WEEKLY");
        this.response.appendInt(this.data != null ? this.data.getOffset() : 0);
        this.response.appendInt(this.data != null ? this.data.getLimit() : 10);
        this.response.appendInt(this.data != null ? this.data.getTotalRows() : 0);

        if (this.data == null) {
            this.response.appendInt(0);
            this.response.appendBoolean(false);
            return this.response;
        }

        this.response.appendInt(this.data.getRows().size());
        for (GamehallLeaderboardRow row : this.data.getRows()) {
            this.serializeRow(row);
        }

        GamehallLeaderboardRow ownRow = this.data.getOwnRow();
        this.response.appendBoolean(ownRow != null);
        if (ownRow != null) {
            this.serializeRow(ownRow);
        }
        return this.response;
    }

    private void serializeRow(GamehallLeaderboardRow row) {
        this.response.appendInt(row.getUserId());
        this.response.appendInt(row.getRank());
        this.response.appendString(row.getUsername());
        this.response.appendString(row.getFigure());
        this.response.appendString(row.getGender());
        this.response.appendInt(row.getPoints());
        this.response.appendInt(row.getWins());
        this.response.appendInt(row.getLosses());
        this.response.appendInt(row.getDraws());
        this.response.appendInt(row.getGamesPlayed());
        this.response.appendBoolean(row.isOwn());
    }
}
