package com.eu.habbo.messages.outgoing.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestUserProgress;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class QuestsMessageComposer extends MessageComposer {
    private final Habbo habbo;
    private final List<Quest> quests;
    private final boolean unknownBoolean;

    public QuestsMessageComposer(Habbo habbo, List<Quest> quests, boolean unknownBoolean) {
        this.habbo = habbo;
        this.quests = quests;
        this.unknownBoolean = unknownBoolean;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.QuestsMessageComposer);
        this.response.appendInt(this.quests.size());

        for (Quest quest : this.quests) {
            QuestUserProgress progress = this.habbo.getHabboStats().getQuestProgress(quest);

            int completedQuestsInCampaign = Emulator.getGameEnvironment().getQuestManager().getCompletedQuestsInCampaign(this.habbo, quest.getCampaignId());
            
            int questCountInCampaign = Emulator.getGameEnvironment().getQuestManager().getQuestCountInCampaign(quest.getCampaignId());

            if (questCountInCampaign > 0 && completedQuestsInCampaign >= questCountInCampaign) {
                this.serializeCompletedCampaign(quest, completedQuestsInCampaign, questCountInCampaign);
                continue;
            }

            quest.serialize(this.response, progress, completedQuestsInCampaign, questCountInCampaign);
        }

        this.response.appendBoolean(this.unknownBoolean);
        return this.response;
    }

    private void serializeCompletedCampaign(Quest quest, int completedQuestsInCampaign, int questCountInCampaign) {
        this.response.appendString(quest.getCampaignCode());
        this.response.appendInt(completedQuestsInCampaign);
        this.response.appendInt(questCountInCampaign);
        this.response.appendInt(quest.getActivityPointType());

        this.response.appendInt(0); // id < 1 = campaign completed on client

        this.response.appendBoolean(false); // accepted
        this.response.appendString("");     // type
        this.response.appendString("");     // imageVersion
        this.response.appendInt(0);         // rewardCurrencyAmount
        this.response.appendString("");     // localizationCode
        this.response.appendInt(0);         // completedSteps
        this.response.appendInt(0);         // totalSteps
        this.response.appendInt(quest.getSortOrder());
        this.response.appendString("");     // catalogPageName
        this.response.appendString(quest.getChainCode());
        this.response.appendBoolean(false); // easy
    }
}