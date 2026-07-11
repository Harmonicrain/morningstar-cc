package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.rooms.RoomUserAction;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserAvatarExpressionEvent extends UserEvent {
    public final Habbo habbo;
    public final RoomUserAction action;

    public UserAvatarExpressionEvent(Habbo habbo, RoomUserAction action) {
        super(habbo);
        this.habbo = habbo;
        this.action = action;
    }
}