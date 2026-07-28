package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.habbohotel.leaderboards.BadgeLeaderboardEntry;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class BadgeLeaderboardMessageComposer extends MessageComposer {
    private final int type;
    private final int rarity;
    private final int page;
    private final int size;
    private final int totalEntries;
    private final List<BadgeLeaderboardEntry> entries;
    private final BadgeLeaderboardEntry ownEntry;

    public BadgeLeaderboardMessageComposer(int type, int rarity, int page, int size, int totalEntries, List<BadgeLeaderboardEntry> entries, BadgeLeaderboardEntry ownEntry) {
        this.type = type;
        this.rarity = rarity;
        this.page = page;
        this.size = size;
        this.totalEntries = totalEntries;
        this.entries = entries;
        this.ownEntry = ownEntry;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BadgeLeaderboardResultMessageComposer);
        this.response.appendInt(this.type);
        this.response.appendInt(this.rarity);
        this.response.appendInt(this.page);
        this.response.appendInt(this.size);
        this.response.appendInt(this.totalEntries);
        this.response.appendInt(this.entries.size());

        for (BadgeLeaderboardEntry entry : this.entries) {
            this.composeEntry(entry);
        }

        this.response.appendBoolean(this.ownEntry != null);
        if (this.ownEntry != null) {
            this.composeEntry(this.ownEntry);
        }

        return this.response;
    }

    private void composeEntry(BadgeLeaderboardEntry entry) {
        this.response.appendInt(entry.getUserId());
        this.response.appendString(entry.getUsername());
        this.response.appendString(entry.getFigure());
        this.response.appendInt(entry.getRank());
        this.response.appendInt(entry.getScore());
    }
}
