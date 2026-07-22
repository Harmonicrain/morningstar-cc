package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.habbohotel.users.Habbo;

interface QuestMidnightTask {
    String getName();

    boolean isEnabled();

    void push(Habbo habbo, QuestManager questManager);
}
