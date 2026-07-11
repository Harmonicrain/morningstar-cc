package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserDanceEvent extends UserEvent {

    public DanceType danceType;

    public UserDanceEvent(Habbo habbo, DanceType danceType) {
        super(habbo);

        this.danceType = danceType;
    }
}
