package com.eu.habbo.habbohotel.communitygoals;

import java.sql.ResultSet;
import java.sql.SQLException;

class CommunityGoalLevel {
    final int level;
    final int scoreThreshold;

    CommunityGoalLevel(ResultSet set) throws SQLException {
        this.level = set.getInt("level");
        this.scoreThreshold = set.getInt("score_threshold");
    }
}
