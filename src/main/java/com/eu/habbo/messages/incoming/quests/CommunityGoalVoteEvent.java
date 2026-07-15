package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalEarnedPrize;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalManager;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalProgress;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotelview.HotelViewHideCommunityVoteButtonComposer;
import com.eu.habbo.messages.outgoing.hotelview.CommunityGoalProgressMessageComposer;
import com.eu.habbo.messages.outgoing.quests.CommunityGoalEarnedPrizesComposer;

import java.util.List;

public class CommunityGoalVoteEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        if (!CommunityGoalManager.isCommunityGoalsEnabled()) {
            return;
        }

        int userId = this.client.getHabbo().getHabboInfo().getId();
        CommunityGoalManager communityGoalManager = Emulator.getGameEnvironment().getCommunityGoalManager();
        boolean acknowledged;

        if (this.packet.bytesAvailable() == 4) {
            acknowledged = communityGoalManager.registerVote(userId, this.packet.readInt());
        } else {
            String goalCode = this.packet.readString();
            acknowledged = communityGoalManager.addContribution(userId, goalCode, 1);
        }

        this.client.sendResponse(new HotelViewHideCommunityVoteButtonComposer(acknowledged || communityGoalManager.shouldHideVoteButton(userId)));
        if (acknowledged) {
            List<CommunityGoalEarnedPrize> votePrizes = communityGoalManager.claimAndDeliverVoteRewards(userId);
            if (!votePrizes.isEmpty()) {
                this.client.sendResponse(new CommunityGoalEarnedPrizesComposer(votePrizes));
            }

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
        }
    }
}
