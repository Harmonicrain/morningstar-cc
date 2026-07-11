package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserCraftProductEvent extends UserEvent {
    public final Habbo habbo;
    public final Item reward;

    public UserCraftProductEvent(Habbo habbo, Item reward) {
        super(habbo);
        this.habbo = habbo;
        this.reward = reward;
    }
}