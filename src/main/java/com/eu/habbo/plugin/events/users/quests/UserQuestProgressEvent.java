package com.eu.habbo.plugin.events.users.quests;

import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestType;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserQuestProgressEvent extends UserQuestEvent {

    public final QuestType questType;

    public final int amount;

    public final int previousSteps;

    public final int completedSteps;

    public UserQuestProgressEvent(Habbo habbo, Quest quest, int amount, int previousSteps, int completedSteps) {
        super(habbo, quest);

        this.questType = quest.getTriggerType();
        this.amount = amount;
        this.previousSteps = previousSteps;
        this.completedSteps = completedSteps;
    }
}
