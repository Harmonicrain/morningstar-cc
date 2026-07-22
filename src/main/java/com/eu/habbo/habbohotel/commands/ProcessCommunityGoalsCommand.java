package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class ProcessCommunityGoalsCommand extends Command {
    public ProcessCommunityGoalsCommand() {
        super("cmd_process_community_goals", Emulator.getTexts().getValue("commands.keys.cmd_process_community_goals").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        Emulator.getGameEnvironment().getCommunityGoalManager().processExpiredGoalRewards();
        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_process_community_goals.processed"), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
