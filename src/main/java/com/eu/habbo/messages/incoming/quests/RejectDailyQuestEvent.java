package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.DailyQuestComposer;
import com.eu.habbo.messages.outgoing.quests.QuestCancelledMessageComposer;

public class RejectDailyQuestEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        Quest rejectedQuest = Emulator.getGameEnvironment().getQuestManager().rejectDailyQuest(this.client.getHabbo());

        if (rejectedQuest != null) {
            this.client.sendResponse(new QuestCancelledMessageComposer(false));
        }

        Quest dailyQuest = rejectedQuest != null
                ? Emulator.getGameEnvironment().getQuestManager().getDailyQuest(this.client.getHabbo(), rejectedQuest.isEasy(), 0)
                : null;
        this.client.sendResponse(new DailyQuestComposer(this.client.getHabbo(), dailyQuest));
    }
}
