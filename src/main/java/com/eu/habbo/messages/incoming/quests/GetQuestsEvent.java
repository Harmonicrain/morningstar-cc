package com.eu.habbo.messages.incoming.quests;

import java.util.List;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.QuestsMessageComposer;

public class GetQuestsEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getVisibleQuests(this.client.getHabbo());
        this.client.sendResponse(new QuestsMessageComposer(this.client.getHabbo(), quests, true));
    }
}
