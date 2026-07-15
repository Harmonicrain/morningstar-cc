package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestUserProgress;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class DailyQuestComposer extends MessageComposer {

    private final Habbo habbo;
    private final Quest quest;

    public DailyQuestComposer(Quest quest) {
        this(null, quest);
    }

    public DailyQuestComposer(Habbo habbo, Quest quest) {
        this.habbo = habbo;
        this.quest = quest;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.DailyQuestComposer);
        this.response.appendBoolean(quest != null);
        if (quest != null) {
            QuestUserProgress progress = this.habbo != null ? this.habbo.getHabboStats().getQuestProgress(quest) : null;
            int completedQuestsInCampaign = this.habbo != null ? Emulator.getGameEnvironment().getQuestManager().getCompletedQuestsInCampaign(this.habbo, quest.getCampaignCode()) : 0;
            int questCountInCampaign = Emulator.getGameEnvironment().getQuestManager().getQuestCountInCampaign(quest.getCampaignCode());

            quest.serialize(this.response, progress, completedQuestsInCampaign, questCountInCampaign); // QuestMessageData 16 fields
            this.response.appendInt(this.habbo != null ? Emulator.getGameEnvironment().getQuestManager().getDailyQuestCount(this.habbo, true) : 0);
            this.response.appendInt(this.habbo != null ? Emulator.getGameEnvironment().getQuestManager().getDailyQuestCount(this.habbo, false) : 0);
        }

        return this.response;
    }
}
