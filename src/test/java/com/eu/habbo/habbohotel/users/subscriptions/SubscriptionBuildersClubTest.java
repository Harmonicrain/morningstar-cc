package com.eu.habbo.habbohotel.users.subscriptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubscriptionBuildersClubTest {
    @Test
    void recognizesReservedBuildersClubItemRange() {
        assertFalse(SubscriptionBuildersClub
                .isBuildersClubVisibleId(SubscriptionBuildersClub.BUILDERS_CLUB_ITEM_ID_START - 1));
        assertTrue(
                SubscriptionBuildersClub.isBuildersClubVisibleId(SubscriptionBuildersClub.BUILDERS_CLUB_ITEM_ID_START));
        assertTrue(SubscriptionBuildersClub.isBuildersClubVisibleId(Integer.MAX_VALUE));
    }
}
