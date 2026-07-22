package com.eu.habbo.plugin.events.users.quests;

import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserQuestCompletedEvent extends UserQuestEvent {

    public final int rewardAmount;

    public final int rewardType;

    public UserQuestCompletedEvent(Habbo habbo, Quest quest, int rewardAmount, int rewardType) {
        super(habbo, quest);

        this.rewardAmount = rewardAmount;
        this.rewardType = rewardType;
    }
}
