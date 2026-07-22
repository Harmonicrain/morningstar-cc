package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestUserProgress;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;
import com.eu.habbo.messages.outgoing.quests.SeasonalQuestsComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class CompleteQuestCommand extends Command {
    public CompleteQuestCommand() {
        super("cmd_complete_quest", Emulator.getTexts().getValue("commands.keys.cmd_complete_quest").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 3) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_complete_quest.usage"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        HabboInfo habboInfo = this.getHabboInfo(gameClient, params[1]);
        if (habboInfo == null) {
            return true;
        }

        Quest quest = this.getQuest(gameClient, params[2]);
        if (quest == null) {
            return true;
        }

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(habboInfo.getId());

        if (habbo != null) {
            QuestUserProgress progress = habbo.getHabboStats().getQuestProgress(quest);

            if (progress == null) {
                progress = new QuestUserProgress(habbo.getHabboInfo().getId(), quest.getId());
                habbo.getHabboStats().setQuestProgress(progress);
            }

            progress.setAccepted(false);
            progress.setCompletedSteps(quest.getTotalSteps());
            progress.setCompletedAt(Emulator.getIntUnixTimestamp());

            if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest)) {
                habbo.getClient().sendResponse(new SeasonalQuestsComposer(habbo,
                        Emulator.getGameEnvironment().getQuestManager().getSeasonalQuests(habbo)));
            } else {
                habbo.getClient().sendResponse(new QuestMessageComposer(habbo, quest));
            }

            this.sendCompletedWhisper(gameClient, quest, habboInfo);
            return true;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users_quests (user_id, quest_id, accepted, completed_steps, completed_at, claimed_at) " +
                             "VALUES (?, ?, 0, ?, ?, 0) " +
                             "ON DUPLICATE KEY UPDATE accepted = 0, completed_steps = VALUES(completed_steps), completed_at = VALUES(completed_at)")) {
            statement.setInt(1, habboInfo.getId());
            statement.setInt(2, quest.getId());
            statement.setInt(3, quest.getTotalSteps());
            statement.setInt(4, Emulator.getIntUnixTimestamp());
            statement.executeUpdate();
        }

        this.sendCompletedWhisper(gameClient, quest, habboInfo);
        return true;
    }

    private void sendCompletedWhisper(GameClient gameClient, Quest quest, HabboInfo habboInfo) {
        this.whisper(gameClient, "commands.succes.cmd_complete_quest.completed",
                "%quest_id%", String.valueOf(quest.getId()),
                "%user%", habboInfo.getUsername());
    }

    private HabboInfo getHabboInfo(GameClient gameClient, String username) {
        HabboInfo habboInfo = HabboManager.getOfflineHabboInfo(username);

        if (habboInfo == null) {
            this.whisper(gameClient, "commands.error.cmd_quest.user_not_found", "%user%", username);
        }

        return habboInfo;
    }

    private Quest getQuest(GameClient gameClient, String value) {
        int questId;

        try {
            questId = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            this.whisper(gameClient, "commands.error.cmd_quest.invalid_quest_id", "%quest_id%", value);
            return null;
        }

        Quest quest = Emulator.getGameEnvironment().getQuestManager().getQuest(questId);

        if (quest == null) {
            this.whisper(gameClient, "commands.error.cmd_quest.quest_not_found", "%quest_id%", String.valueOf(questId));
        }

        return quest;
    }

    private void whisper(GameClient gameClient, String key, String... replacements) {
        String message = Emulator.getTexts().getValue(key);

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }

        gameClient.getHabbo().whisper(message, RoomChatMessageBubbles.ALERT);
    }
}
