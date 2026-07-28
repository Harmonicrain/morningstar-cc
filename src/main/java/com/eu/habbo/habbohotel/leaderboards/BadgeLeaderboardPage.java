package com.eu.habbo.habbohotel.leaderboards;

import java.util.List;

public class BadgeLeaderboardPage {
    private final int totalEntries;
    private final List<BadgeLeaderboardEntry> entries;
    private final BadgeLeaderboardEntry ownEntry;

    public BadgeLeaderboardPage(int totalEntries, List<BadgeLeaderboardEntry> entries, BadgeLeaderboardEntry ownEntry) {
        this.totalEntries = totalEntries;
        this.entries = entries;
        this.ownEntry = ownEntry;
    }

    public int getTotalEntries() {
        return this.totalEntries;
    }

    public List<BadgeLeaderboardEntry> getEntries() {
        return this.entries;
    }

    public BadgeLeaderboardEntry getOwnEntry() {
        return this.ownEntry;
    }
}
