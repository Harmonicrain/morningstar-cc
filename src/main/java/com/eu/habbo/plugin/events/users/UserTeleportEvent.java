package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

public class UserTeleportEvent extends UserEvent {
    public final Habbo habbo;
    public final HabboItem item;

    public UserTeleportEvent(Habbo habbo, HabboItem item) {
        super(habbo);
        this.habbo = habbo;
        this.item = item;
    }
}