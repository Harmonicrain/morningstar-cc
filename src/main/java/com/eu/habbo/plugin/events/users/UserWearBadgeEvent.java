package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboBadge;

public class UserWearBadgeEvent extends UserEvent {
    public final HabboBadge badge;

    public UserWearBadgeEvent(Habbo habbo, HabboBadge badge) {
        super(habbo);
        this.badge = badge;
    }
}
