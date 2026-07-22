package com.eu.habbo.habbohotel.communitygoals;

import com.eu.habbo.Emulator;

import java.sql.ResultSet;
import java.sql.SQLException;

class CommunityGoal {
    final String code;
    final String goalType;
    final int totalScore;
    final int startTimestamp;
    final int durationSeconds;
    final int resultOption;

    CommunityGoal(ResultSet set) throws SQLException {
        this.code = set.getString("code");
        this.goalType = set.getString("goal_type");
        this.totalScore = set.getInt("total_score");
        this.startTimestamp = set.getInt("start_timestamp");
        this.durationSeconds = set.getInt("duration_seconds");
        this.resultOption = set.getInt("result_option");
    }

    int getTimeLeft() {
        if (this.startTimestamp == 0) {
            return this.durationSeconds;
        }

        return Math.max((this.startTimestamp + this.durationSeconds) - Emulator.getIntUnixTimestamp(), 0);
    }

    boolean isAcceptingContributions() {
        if (this.startTimestamp == 0) {
            return true;
        }

        int now = Emulator.getIntUnixTimestamp();
        return this.startTimestamp <= now && this.startTimestamp + this.durationSeconds > now;
    }

    boolean isVersus() {
        return "versus_all".equalsIgnoreCase(this.goalType) || "versus_winner".equalsIgnoreCase(this.goalType);
    }

    boolean isVersusAll() {
        return "versus_all".equalsIgnoreCase(this.goalType);
    }

    boolean isVersusWinner() {
        return "versus_winner".equalsIgnoreCase(this.goalType);
    }
}
