package com.eu.habbo.habbohotel.leaderboards;

public class BadgeLeaderboardEntry {
    private final int userId;
    private final String username;
    private final String figure;
    private final int rank;
    private final int score;

    public BadgeLeaderboardEntry(int userId, String username, String figure, int rank, int score) {
        this.userId = userId;
        this.username = username;
        this.figure = figure;
        this.rank = rank;
        this.score = score;
    }

    public int getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getFigure() {
        return this.figure;
    }

    public int getRank() {
        return this.rank;
    }

    public int getScore() {
        return this.score;
    }
}
