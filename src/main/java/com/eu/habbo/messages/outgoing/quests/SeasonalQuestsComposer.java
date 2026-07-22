package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestUserProgress;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class SeasonalQuestsComposer extends MessageComposer {
    private final Habbo habbo;
    private final List<Quest> quests;

    public SeasonalQuestsComposer(Habbo habbo, List<Quest> quests) {
        this.habbo = habbo;
        this.quests = quests;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.SeasonalQuestsComposer);
        this.response.appendInt(this.quests.size());

        for (Quest quest : this.quests) {
            QuestUserProgress progress = this.habbo.getHabboStats().getQuestProgress(quest);
            int completedQuestsInCampaign = Emulator.getGameEnvironment().getQuestManager().getCompletedQuestsInCampaign(this.habbo, quest.getCampaignCode());
            int questCountInCampaign = Emulator.getGameEnvironment().getQuestManager().getQuestCountInCampaign(quest.getCampaignCode());

            if (progress != null && progress.isCompleted(quest)) {
                this.serializeCompletedQuest(quest, completedQuestsInCampaign, questCountInCampaign);
                continue;
            }

            quest.serialize(this.response, progress, completedQuestsInCampaign, questCountInCampaign);
        }

        return this.response;
    }

    private void serializeCompletedQuest(Quest quest, int completedQuestsInCampaign, int questCountInCampaign) {
        this.response.appendString(quest.getCampaignCode());
        this.response.appendInt(completedQuestsInCampaign);
        this.response.appendInt(questCountInCampaign);
        this.response.appendInt(quest.getActivityPointType());

        this.response.appendInt(0); // id < 1 makes the seasonal calendar render this day as completed.

        this.response.appendBoolean(false);
        this.response.appendString("");
        this.response.appendString("");
        this.response.appendInt(0);
        this.response.appendString("");
        this.response.appendInt(0);
        this.response.appendInt(0);
        this.response.appendInt(quest.getSortOrder());
        this.response.appendString("");
        this.response.appendString(quest.getClientChainCode());
        this.response.appendBoolean(quest.isEasy());
    }
}
