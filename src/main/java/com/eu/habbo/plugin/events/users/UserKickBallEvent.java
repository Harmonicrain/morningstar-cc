package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

public class UserKickBallEvent extends UserEvent {
    public final Habbo habbo;
    public final HabboItem ball;

    public UserKickBallEvent(Habbo habbo, HabboItem ball) {
        super(habbo);

        this.habbo = habbo;
        this.ball = ball;
    }
}