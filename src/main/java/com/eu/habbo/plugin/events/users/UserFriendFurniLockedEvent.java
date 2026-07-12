package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

public class UserFriendFurniLockedEvent extends UserEvent {
    public final Habbo habbo;
    public final Habbo target;
    public final HabboItem item;

    public UserFriendFurniLockedEvent(Habbo habbo, Habbo target, HabboItem item) {
        super(habbo);

        this.habbo = habbo;
        this.target = target;
        this.item = item;
    }
}