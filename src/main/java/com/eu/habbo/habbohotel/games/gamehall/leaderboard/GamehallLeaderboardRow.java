package com.eu.habbo.habbohotel.games.gamehall.leaderboard;

public class GamehallLeaderboardRow {
    private final int userId;
    private final int rank;
    private final String username;
    private final String figure;
    private final String gender;
    private final int points;
    private final int wins;
    private final int losses;
    private final int draws;
    private final int gamesPlayed;
    private final boolean own;

    public GamehallLeaderboardRow(int userId, int rank, String username, String figure, String gender,
                                  int points, int wins, int losses, int draws, int gamesPlayed, boolean own) {
        this.userId = userId;
        this.rank = rank;
        this.username = username == null ? "" : username;
        this.figure = figure == null ? "" : figure;
        this.gender = gender == null ? "" : gender;
        this.points = points;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.gamesPlayed = gamesPlayed;
        this.own = own;
    }

    public int getUserId() {
        return this.userId;
    }

    public int getRank() {
        return this.rank;
    }

    public String getUsername() {
        return this.username;
    }

    public String getFigure() {
        return this.figure;
    }

    public String getGender() {
        return this.gender;
    }

    public int getPoints() {
        return this.points;
    }

    public int getWins() {
        return this.wins;
    }

    public int getLosses() {
        return this.losses;
    }

    public int getDraws() {
        return this.draws;
    }

    public int getGamesPlayed() {
        return this.gamesPlayed;
    }

    public boolean isOwn() {
        return this.own;
    }
}
