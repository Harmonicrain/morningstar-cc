package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestUserProgress;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class QuestMessageComposer extends MessageComposer {
    private final Habbo habbo;
    private final Quest quest;

    public QuestMessageComposer(Habbo habbo, Quest quest) {
        this.habbo = habbo;
        this.quest = quest;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestMessageComposer);
        QuestUserProgress progress = this.habbo.getHabboStats().getQuestProgress(this.quest);

        int completedQuestsInCampaign = Emulator.getGameEnvironment().getQuestManager().getCompletedQuestsInCampaign(this.habbo, this.quest.getCampaignId());

        int questCountInCampaign = Emulator.getGameEnvironment().getQuestManager().getQuestCountInCampaign(this.quest.getCampaignId());

        this.quest.serialize(this.response, progress, completedQuestsInCampaign, questCountInCampaign);

        return this.response;
    }
}