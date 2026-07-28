package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.habbohotel.leaderboards.BadgeLeaderboardManager;
import com.eu.habbo.habbohotel.leaderboards.BadgeLeaderboardPage;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.BadgeLeaderboardMessageComposer;

public class BadgeLeaderboardMessageEvent extends MessageHandler {
    private static final int MAX_CHUNK_SIZE = 50;

    @Override
    public void handle() throws Exception {
        int type = this.packet.readInt();
        int rarity = this.packet.readInt();
        int chunk = Math.max(0, this.packet.readInt());
        int size = Math.max(1, Math.min(MAX_CHUNK_SIZE, this.packet.readInt()));

        if (type < BadgeLeaderboardManager.TYPE_TOTAL_BADGES || type > BadgeLeaderboardManager.TYPE_ACHIEVEMENT_SCORE) {
            type = BadgeLeaderboardManager.TYPE_TOTAL_BADGES;
        }

        if (type != BadgeLeaderboardManager.TYPE_BADGES_BY_RARITY) {
            rarity = -1;
        } else {
            rarity = Math.max(BadgeLeaderboardManager.MIN_RARITY, Math.min(BadgeLeaderboardManager.MAX_RARITY, rarity));
        }

        BadgeLeaderboardPage page = BadgeLeaderboardManager.getInstance().getPage(
                type,
                rarity,
                chunk,
                size,
                this.client.getHabbo().getHabboInfo().getId());

        this.client.sendResponse(new BadgeLeaderboardMessageComposer(
                type,
                rarity,
                chunk,
                size,
                page.getTotalEntries(),
                page.getEntries(),
                page.getOwnEntry()));
    }
}
