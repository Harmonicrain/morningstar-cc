package com.eu.habbo.habbohotel.quests;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.quests.QuestCancelledMessageComposer;
import com.eu.habbo.messages.outgoing.quests.SeasonalQuestsComposer;

class SeasonalQuestMidnightTask implements QuestMidnightTask {
    @Override
    public String getName() {
        return "seasonal quests";
    }

    @Override
    public boolean isEnabled() {
        return QuestManager.isSeasonalQuestSystemEnabled();
    }

    @Override
    public void push(Habbo habbo, QuestManager questManager) {
        if (this.expireOutdatedSeasonalQuest(habbo, questManager)) {
            habbo.getClient().sendResponse(new QuestCancelledMessageComposer(true));
        }

        habbo.getClient().sendResponse(new SeasonalQuestsComposer(habbo, questManager.getSeasonalQuests(habbo)));
    }

    private boolean expireOutdatedSeasonalQuest(Habbo habbo, QuestManager questManager) {
        boolean expired = false;

        synchronized (habbo.getHabboStats().getQuestProgress()) {
            for (QuestUserProgress progress : habbo.getHabboStats().getQuestProgress().values()) {
                if (!progress.isAccepted()) {
                    continue;
                }

                Quest quest = questManager.getQuest(progress.getQuestId());

                if (!questManager.isSeasonalQuest(quest)) {
                    continue;
                }

                if (questManager.isCurrentSeasonalQuest(quest)) {
                    continue;
                }

                progress.setAccepted(false);
                expired = true;
            }
        }

        return expired;
    }
}
