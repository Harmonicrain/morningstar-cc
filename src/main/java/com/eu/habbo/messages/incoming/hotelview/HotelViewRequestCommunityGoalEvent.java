package com.eu.habbo.messages.incoming.hotelview;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalEarnedPrize;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalManager;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalProgress;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotelview.CommunityGoalProgressMessageComposer;
import com.eu.habbo.messages.outgoing.hotelview.HotelViewHideCommunityVoteButtonComposer;
import com.eu.habbo.messages.outgoing.quests.CommunityGoalEarnedPrizesComposer;

import java.util.ArrayList;
import java.util.List;

public class HotelViewRequestCommunityGoalEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!CommunityGoalManager.isCommunityGoalsEnabled()) {
            return;
        }

        CommunityGoalManager communityGoalManager = Emulator.getGameEnvironment().getCommunityGoalManager();
        int userId = this.client.getHabbo().getHabboInfo().getId();
        CommunityGoalProgress progress = communityGoalManager.getProgress(userId);

        this.client.sendResponse(new CommunityGoalProgressMessageComposer(
                progress.isAchieved(),
                progress.getPersonalContributionScore(),
                progress.getPersonalRank(),
                progress.getTotalAmount(),
                progress.getCommunityHighestAchievedLevel(),
                progress.getScoreRemainingUntilNextLevel(),
                progress.getPercentCompletionTowardsNextLevel(),
                progress.getCompetitionName(),
                progress.getTimeLeft(),
                progress.getRankData()));

        if (communityGoalManager.shouldHideVoteButton(userId)) {
            this.client.sendResponse(new HotelViewHideCommunityVoteButtonComposer(true));
        }

        List<CommunityGoalEarnedPrize> earnedPrizes = communityGoalManager.getEarnedPrizes(userId);
        List<CommunityGoalEarnedPrize> claimedPrizes = new ArrayList<>();

        if (!earnedPrizes.isEmpty()) {
            for (CommunityGoalEarnedPrize earnedPrize : earnedPrizes) {
                if (!communityGoalManager.claimAndDeliverEarnedPrize(userId, earnedPrize)) {
                    continue;
                }

                claimedPrizes.add(earnedPrize);
            }

            if (!claimedPrizes.isEmpty()) {
                this.client.sendResponse(new CommunityGoalEarnedPrizesComposer(claimedPrizes));
            }
        }
    }
}
