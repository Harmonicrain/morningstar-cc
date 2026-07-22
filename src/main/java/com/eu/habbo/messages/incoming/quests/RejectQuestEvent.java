package com.eu.habbo.messages.incoming.quests;

import java.util.List;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.DailyQuestComposer;
import com.eu.habbo.messages.outgoing.quests.QuestCancelledMessageComposer;
import com.eu.habbo.messages.outgoing.quests.QuestsMessageComposer;
import com.eu.habbo.messages.outgoing.quests.SeasonalQuestsComposer;

public class RejectQuestEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        Quest quest = QuestManager.rejectActiveQuest(this.client.getHabbo());

        if (quest == null)
            return;

        if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest)) {
            List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getSeasonalQuests(habbo);
            this.client.sendResponse(new QuestCancelledMessageComposer(false));
            this.client.sendResponse(new SeasonalQuestsComposer(habbo, quests));
            return;
        }

        List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getVisibleQuests(this.client.getHabbo());

        this.client.sendResponse(new QuestsMessageComposer(habbo, quests, true));
        this.client.sendResponse(new QuestCancelledMessageComposer(false));

        if (Emulator.getGameEnvironment().getQuestManager().isDailyQuest(quest)) {
            Quest dailyQuest = Emulator.getGameEnvironment().getQuestManager().getDailyQuest(habbo, quest.isEasy(), 0);
            this.client.sendResponse(new DailyQuestComposer(habbo, dailyQuest));
        }
    }
}
