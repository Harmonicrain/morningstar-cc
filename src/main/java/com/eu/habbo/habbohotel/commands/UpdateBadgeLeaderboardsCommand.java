package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.leaderboards.BadgeLeaderboardManager;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class UpdateBadgeLeaderboardsCommand extends Command {
    public UpdateBadgeLeaderboardsCommand() {
        super(
                "cmd_update_badge_leaderboards",
                Emulator.getTexts()
                        .getValue("commands.keys.cmd_update_badge_leaderboards")
                        .split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) {
        BadgeLeaderboardManager manager = BadgeLeaderboardManager.getInstance();

        if (!manager.rebuildAggregatesAsync(gameClient.getHabbo().getHabboInfo().getId())) {
            gameClient.getHabbo().whisper(
                    Emulator.getTexts().getValue(
                            "commands.error.cmd_update_badge_leaderboards.running"),
                    RoomChatMessageBubbles.ALERT);
            return true;
        }

        gameClient.getHabbo().whisper(
                Emulator.getTexts().getValue(
                        "commands.success.cmd_update_badge_leaderboards.started"),
                RoomChatMessageBubbles.ALERT);
        return true;
    }
}
