package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;

public class AcceptQuestEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int questId = this.packet.readInt();

        Quest quest = Emulator.getGameEnvironment().getQuestManager().getQuest(questId);

        if (quest == null)
            return;

        QuestManager.acceptQuest(this.client.getHabbo(), quest);
        this.client.sendResponse(new QuestMessageComposer(this.client.getHabbo(), quest));

        // On Habbo, the quest window stays open after accepting a quest. This is a workaround.
        //List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getVisibleQuests(this.client.getHabbo());
        //this.client.sendResponse(new QuestsMessageComposer(this.client.getHabbo(), quests, true));
    }
}
