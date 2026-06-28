package com.eu.habbo.habbohotel.games.gamehall;

import com.eu.habbo.Emulator;

public enum GamehallGameType {
    TICTACTOE("TicTacToe", 1, 2, "gamehall.tictactoe.enabled"),
    CHESS("Chess", 2, 2, "gamehall.chess.enabled"),
    BATTLESHIPS("BattleShip", 1, 2, "gamehall.battleships.enabled"),
    POKER("Poker", 1, 4, "gamehall.poker.enabled");

    private final String fuseToken;
    private final int minSeats;
    private final int maxSeats;
    private final String settingKey;

    GamehallGameType(String fuseToken, int minSeats, int maxSeats, String settingKey) {
        this.fuseToken = fuseToken;
        this.minSeats = minSeats;
        this.maxSeats = maxSeats;
        this.settingKey = settingKey;
    }

    public String getFuseToken() {
        return this.fuseToken;
    }

    public int getMinSeats() {
        return this.minSeats;
    }

    public int getMaxSeats() {
        return this.maxSeats;
    }

    public String getSettingKey() {
        return this.settingKey;
    }

    /**
     * Whether this game is switched on in emulator_settings. Defaults to disabled when the row is
     * missing, so an uncoded game never opens its (empty) board — the chair just behaves as a seat.
     */
    public boolean isEnabled() {
        return Emulator.getConfig() != null && Emulator.getConfig().getBoolean(this.settingKey, false);
    }
}
