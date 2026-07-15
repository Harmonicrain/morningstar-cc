package com.eu.habbo.habbohotel.communitygoals;

public enum CommunityGoalTriggerType {
    CATALOGUE_PURCHASE("catalogue_purchase"),
    CREDITS_SPENT("credits_spent"),
    POINTS_SPENT("points_spent"),
    DUCKETS_SPENT("duckets_spent"),
    ECOTRON_RECYCLE("ecotron_recycle"),
    CONCURRENT_USERS("concurrent_users");

    private final String code;

    CommunityGoalTriggerType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
