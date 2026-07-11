package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserSwimEvent extends UserEvent {
    public final Habbo habbo;

    public UserSwimEvent(Habbo habbo) {
        super(habbo);
        this.habbo = habbo;
    }
}