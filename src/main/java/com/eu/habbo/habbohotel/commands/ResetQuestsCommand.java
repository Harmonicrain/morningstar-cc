package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.quests.Quest;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class ResetQuestsCommand extends Command {
    public ResetQuestsCommand() {
        super("cmd_reset_quest", Emulator.getTexts().getValue("commands.keys.cmd_reset_quest").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_reset_quest.usage"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        HabboInfo habboInfo = this.getHabboInfo(gameClient, params[1]);
        if (habboInfo == null) {
            return true;
        }

        Integer questId = null;

        if (params.length >= 3) {
            Quest quest = this.getQuest(gameClient, params[2]);
            if (quest == null) {
                return true;
            }

            questId = quest.getId();
        }

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(habboInfo.getId());

        if (habbo != null) {
            if (questId == null) {
                habbo.getHabboStats().getQuestProgress().clear();
            } else {
                habbo.getHabboStats().getQuestProgress().remove(questId);
            }
        }

        String whereClause = questId == null ? " WHERE user_id = ?" : " WHERE user_id = ? AND quest_id = ?";

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement progressStatement = connection.prepareStatement("DELETE FROM users_quests" + whereClause);
             PreparedStatement queueStatement = connection.prepareStatement("DELETE FROM users_quests_queue" + whereClause)) {
            progressStatement.setInt(1, habboInfo.getId());
            queueStatement.setInt(1, habboInfo.getId());

            if (questId != null) {
                progressStatement.setInt(2, questId);
                queueStatement.setInt(2, questId);
            }

            progressStatement.executeUpdate();
            queueStatement.executeUpdate();
        }

        this.whisper(gameClient, "commands.succes.cmd_reset_quest.reset",
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
