package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.DailyQuestComposer;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;

public class ActivateQuestEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int questId = this.packet.readInt();

        Quest quest = Emulator.getGameEnvironment().getQuestManager().getQuest(questId);

        if (quest == null)
            return;

        if (!Emulator.getGameEnvironment().getQuestManager().isDailyQuest(quest))
            return;

        if (!QuestManager.acceptQuest(this.client.getHabbo(), quest))
            return;

        this.client.sendResponse(new DailyQuestComposer(this.client.getHabbo(), quest));
        this.client.sendResponse(new QuestMessageComposer(this.client.getHabbo(), quest));
    }
}
