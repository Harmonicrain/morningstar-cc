package com.eu.habbo.plugin.events.users.quests;

import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public abstract class UserQuestEvent extends UserEvent {

    public final Quest quest;

    public UserQuestEvent(Habbo habbo, Quest quest) {
        super(habbo);

        this.quest = quest;
    }
}
