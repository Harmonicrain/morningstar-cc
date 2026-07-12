package com.eu.habbo.messages.incoming.quests;

import java.util.List;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.QuestCancelledMessageComposer;
import com.eu.habbo.messages.outgoing.quests.QuestsMessageComposer;

public class RejectQuestEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        Quest quest = QuestManager.rejectActiveQuest(this.client.getHabbo());

        if (quest == null)
            return;

        List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getVisibleQuests(this.client.getHabbo());

        this.client.sendResponse(new QuestsMessageComposer(habbo, quests, true));
        this.client.sendResponse(new QuestCancelledMessageComposer(false));
    }
}
