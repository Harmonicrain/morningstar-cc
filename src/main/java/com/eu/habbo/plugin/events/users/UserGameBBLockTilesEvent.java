package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserGameBBLockTilesEvent extends UserEvent {
    public final Habbo habbo;
    public final int tileCount;
    public final boolean area;

    public UserGameBBLockTilesEvent(Habbo habbo, int tileCount, boolean area) {
        super(habbo);

        this.habbo = habbo;
        this.tileCount = tileCount;
        this.area = area;
    }
}