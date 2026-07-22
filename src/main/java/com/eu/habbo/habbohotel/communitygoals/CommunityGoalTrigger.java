package com.eu.habbo.habbohotel.communitygoals;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

class CommunityGoalTrigger {
    final String goalCode;
    private final String contributionMode;
    private final int catalogPageId;
    private final int catalogItemId;
    private final int baseItemId;
    private final int currencyType;
    private final int minAmount;
    private final int maxAmount;

    CommunityGoalTrigger(ResultSet set) throws SQLException {
        this.goalCode = set.getString("goal_code");
        this.contributionMode = set.getString("contribution_mode");
        this.catalogPageId = set.getInt("catalog_page_id");
        this.catalogItemId = set.getInt("catalog_item_id");
        this.baseItemId = set.getInt("base_item_id");
        this.currencyType = set.getInt("currency_type");
        this.minAmount = set.getInt("min_amount");
        this.maxAmount = set.getInt("max_amount");
    }

    boolean matches(int amount, int catalogPageId, int catalogItemId, List<Integer> baseItemIds, int currencyType) {
        if (this.goalCode == null || this.goalCode.isEmpty()) {
            return false;
        }

        if (this.minAmount > 0 && amount < this.minAmount) {
            return false;
        }

        if (this.maxAmount > 0 && amount > this.maxAmount) {
            return false;
        }

        if (this.catalogPageId > 0 && this.catalogPageId != catalogPageId) {
            return false;
        }

        if (this.catalogItemId > 0 && this.catalogItemId != catalogItemId) {
            return false;
        }

        if (this.baseItemId > 0 && (baseItemIds == null || !baseItemIds.contains(this.baseItemId))) {
            return false;
        }

        return this.currencyType < 0 || this.currencyType == currencyType;
    }

    int getContribution(int amount) {
        if ("event".equalsIgnoreCase(this.contributionMode)) {
            return 1;
        }

        return amount;
    }
}
