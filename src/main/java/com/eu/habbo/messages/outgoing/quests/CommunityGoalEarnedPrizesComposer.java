package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.habbohotel.communitygoals.CommunityGoalEarnedPrize;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.Collections;
import java.util.List;

public class CommunityGoalEarnedPrizesComposer extends MessageComposer {
    private final List<CommunityGoalEarnedPrize> prizes;

    public CommunityGoalEarnedPrizesComposer(List<CommunityGoalEarnedPrize> prizes) {
        this.prizes = prizes != null ? prizes : Collections.emptyList();
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.CommunityGoalEarnedPrizesComposer);
        this.response.appendInt(this.prizes.size());

        for (CommunityGoalEarnedPrize prize : this.prizes) {
            this.response.appendInt(prize.prizeId);
            this.response.appendString(prize.prizeType);
            this.response.appendInt(prize.userRank);
            this.response.appendString(prize.badgeCode);
            this.response.appendBoolean(prize.badge);
            this.response.appendString(prize.localizedName);
        }

        return this.response;
    }
}
