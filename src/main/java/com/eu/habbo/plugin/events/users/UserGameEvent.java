package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.games.Game;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserGameEvent extends UserEvent {
    public final Habbo habbo;
    public final Game game;
    public final String gameType;
    public final boolean won;

    public UserGameEvent(Habbo habbo, Game game, String gameType, boolean won) {
        super(habbo);

        this.habbo = habbo;
        this.game = game;
        this.gameType = gameType;
        this.won = won;
    }
}