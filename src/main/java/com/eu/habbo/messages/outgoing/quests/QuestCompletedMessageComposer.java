package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestUserProgress;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class QuestCompletedMessageComposer extends MessageComposer {
    private final Habbo habbo;
    private final Quest quest;
    private final boolean unknownBoolean;

    public QuestCompletedMessageComposer(Habbo habbo, Quest quest, boolean unknownBoolean) {
        this.habbo = habbo;
        this.quest = quest;
        this.unknownBoolean = unknownBoolean;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestCompletedMessageComposer);

        if (this.habbo == null || this.quest == null)
            return this.response;

        QuestUserProgress progress = this.habbo.getHabboStats().getQuestProgress(this.quest);
        int completedQuestsInCampaign = Emulator.getGameEnvironment().getQuestManager().getCompletedQuestsInCampaign(this.habbo, this.quest.getCampaignId());
        int questCountInCampaign = Emulator.getGameEnvironment().getQuestManager().getQuestCountInCampaign(this.quest.getCampaignId());

        this.quest.serialize(this.response, progress, completedQuestsInCampaign, questCountInCampaign);

        this.response.appendBoolean(this.unknownBoolean);

        return this.response;
    }
}