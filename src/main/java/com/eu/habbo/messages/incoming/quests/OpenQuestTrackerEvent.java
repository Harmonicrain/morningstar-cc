package com.eu.habbo.messages.incoming.quests;

import java.util.List;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;
import com.eu.habbo.messages.outgoing.quests.QuestsMessageComposer;

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

        List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getVisibleQuests(this.client.getHabbo());
        this.client.sendResponse(new QuestsMessageComposer(this.client.getHabbo(), quests, true));
    }
}