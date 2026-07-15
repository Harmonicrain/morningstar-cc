package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.DailyQuestComposer;

public class GetDailyQuestEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        boolean easy = this.packet.readBoolean();
        int index = this.packet.readInt();
        Quest quest = Emulator.getGameEnvironment().getQuestManager().getDailyQuest(this.client.getHabbo(), easy, index);

        this.client.sendResponse(new DailyQuestComposer(this.client.getHabbo(), quest));
    }
}
