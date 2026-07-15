package com.eu.habbo.plugin.events.users.quests;

import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserQuestAcceptedEvent extends UserQuestEvent {

    public UserQuestAcceptedEvent(Habbo habbo, Quest quest) {
        super(habbo, quest);
    }
}
