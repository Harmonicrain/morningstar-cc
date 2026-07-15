package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;

public class StartCampaignEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        String campaignCode = this.packet.readString();
        Quest quest = Emulator.getGameEnvironment().getQuestManager().getVisibleQuestForCampaign(this.client.getHabbo(), campaignCode);

        if (quest == null)
            return;

        if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest) && !Emulator.getGameEnvironment().getQuestManager().isCurrentSeasonalQuest(quest))
            return;

        if (!QuestManager.acceptQuest(this.client.getHabbo(), quest))
            return;

        this.client.sendResponse(new QuestMessageComposer(this.client.getHabbo(), quest));
    }
}
