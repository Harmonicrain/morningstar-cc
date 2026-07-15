package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.quests.QuestManager;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.eu.habbo.messages.outgoing.quests.DailyQuestComposer;
import com.eu.habbo.messages.outgoing.quests.QuestMessageComposer;
import com.eu.habbo.messages.outgoing.quests.SeasonalQuestsComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class GiveQuestCommand extends Command {
    public GiveQuestCommand() {
        super("cmd_give_quest", Emulator.getTexts().getValue("commands.keys.cmd_give_quest").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 3) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_give_quest.usage"), RoomChatMessageBubbles.ALERT);
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
            if (QuestManager.forceAcceptQuest(habbo, quest)) {
                if (Emulator.getGameEnvironment().getQuestManager().isDailyQuest(quest)) {
                    habbo.getClient().sendResponse(new DailyQuestComposer(habbo, quest));
                }

                if (Emulator.getGameEnvironment().getQuestManager().isSeasonalQuest(quest)) {
                    habbo.getClient().sendResponse(new SeasonalQuestsComposer(habbo,
                            Emulator.getGameEnvironment().getQuestManager().getSeasonalQuests(habbo)));
                }

                habbo.getClient().sendResponse(new QuestMessageComposer(habbo, quest));
                this.whisper(gameClient, "commands.succes.cmd_give_quest.given",
                        "%quest_id%", String.valueOf(quest.getId()),
                        "%user%", habboInfo.getUsername());
            } else {
                this.whisper(gameClient, "commands.error.cmd_give_quest.failed",
                        "%quest_id%", String.valueOf(quest.getId()),
                        "%user%", habboInfo.getUsername());
            }

            return true;
        }

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement updateStatement = connection.prepareStatement("UPDATE users_quests SET accepted = 0 WHERE user_id = ?")) {
                updateStatement.setInt(1, habboInfo.getId());
                updateStatement.executeUpdate();
            }

            try (PreparedStatement insertStatement = connection.prepareStatement(
                    "INSERT INTO users_quests (user_id, quest_id, accepted, completed_steps, completed_at, claimed_at) " +
                            "VALUES (?, ?, 1, 0, 0, 0) " +
                            "ON DUPLICATE KEY UPDATE accepted = 1, completed_steps = 0, completed_at = 0, claimed_at = 0")) {
                insertStatement.setInt(1, habboInfo.getId());
                insertStatement.setInt(2, quest.getId());
                insertStatement.executeUpdate();
            }

            connection.commit();
        }

        this.whisper(gameClient, "commands.succes.cmd_give_quest.queued",
                "%quest_id%", String.valueOf(quest.getId()),
                "%user%", habboInfo.getUsername());
        return true;
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
