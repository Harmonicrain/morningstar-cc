package com.eu.habbo.habbohotel.communitygoals;

public class CommunityGoalEarnedPrize {
    public final int prizeId;
    public final String prizeType;
    public final int userRank;
    public final String badgeCode;
    public final boolean badge;
    public final String localizedName;

    public CommunityGoalEarnedPrize(int prizeId, String prizeType, int userRank, String badgeCode, boolean badge, String localizedName) {
        this.prizeId = prizeId;
        this.prizeType = prizeType;
        this.userRank = userRank;
        this.badgeCode = badgeCode;
        this.badge = badge;
        this.localizedName = localizedName;
    }
}
