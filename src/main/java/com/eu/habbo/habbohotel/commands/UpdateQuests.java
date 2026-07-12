package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdateQuests extends Command {
    public UpdateQuests() {
        super("cmd_update_quests", Emulator.getTexts().getValue("commands.keys.cmd_update_quests").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Emulator.getGameEnvironment().getQuestManager().reload();
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_quests.updated"), RoomChatMessageBubbles.ALERT);

        return true;
    }
}