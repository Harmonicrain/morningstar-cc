package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.users.Habbo;

public class UserReceiveHandItemEvent extends UserEvent {
    
    public final Habbo habbo;
    public final int handItemId;

    public UserReceiveHandItemEvent(Habbo habbo, int handItemId) {
        super(habbo);
        
        this.habbo = habbo;
        this.handItemId = handItemId;
    }
}