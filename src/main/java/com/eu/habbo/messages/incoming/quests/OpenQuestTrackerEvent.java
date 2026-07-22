package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;

public class OpenQuestTrackerEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        Quest quest = QuestManager.getActiveQuest(this.client.getHabbo());

        if (quest == null) {
            quest = QuestManager.activateNextQuest(this.client.getHabbo());
        }

        if (quest == null)
            return;

        this.client.sendResponse(new QuestMessageComposer(this.client.getHabbo(), quest));
    }
}
