package com.eu.habbo.messages.incoming.games.gamehall;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.games.gamehall.leaderboard.GamehallLeaderboardData;
import com.eu.habbo.habbohotel.games.gamehall.GamehallManager;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.games.gamehall.OpenGamehallLeaderboardMessageComposer;

/**
 * Client clicked the Games Hall high-score prompt. The server validates that the user is still in a
 * playable Games Hall room before sending the requested leaderboard slice.
 */
public class RequestGamehallLeaderboardMessageEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 500;
    }

    @Override
    public void handle() throws Exception {
        Habbo habbo = this.client.getHabbo();
        if (habbo == null) {
            return;
        }

        Room room = habbo.getHabboInfo().getCurrentRoom();
        String model = room != null && room.getLayout() != null ? room.getLayout().getName() : null;
        if (!GamehallManager.isGamehallModel(model)) {
            return;
        }

        String gameType = "ALL";
        String period = "WEEKLY";
        int offset = 0;
        int limit = 10;

        if (this.packet.bytesAvailable() > 0) {
            gameType = this.packet.readString();
        }
        if (this.packet.bytesAvailable() > 0) {
            period = this.packet.readString();
        }
        if (this.packet.bytesAvailable() > 0) {
            offset = this.packet.readInt();
        }
        if (this.packet.bytesAvailable() > 0) {
            limit = this.packet.readInt();
        }

        if (Emulator.getGameEnvironment() == null || Emulator.getGameEnvironment().getGamehallLeaderboardManager() == null) {
            return;
        }

        GamehallLeaderboardData data = Emulator.getGameEnvironment().getGamehallLeaderboardManager()
                .getLeaderboard(gameType, period, offset, limit, habbo);
        this.client.sendResponse(new OpenGamehallLeaderboardMessageComposer(data));
    }
}
