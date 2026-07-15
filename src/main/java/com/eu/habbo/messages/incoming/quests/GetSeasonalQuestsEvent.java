package com.eu.habbo.messages.incoming.quests;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.quests.SeasonalQuestsComposer;

import java.util.List;

public class GetSeasonalQuestsEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        List<Quest> quests = Emulator.getGameEnvironment().getQuestManager().getSeasonalQuests(this.client.getHabbo());
        this.client.sendResponse(new SeasonalQuestsComposer(this.client.getHabbo(), quests));
    }
}
