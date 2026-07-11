package com.eu.habbo.plugin.events.users.pets;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.plugin.events.users.UserEvent;

public class UserPetEatEvent extends UserEvent {
    public final Habbo habbo;
    public final Pet pet;
    public final HabboItem item;

    public UserPetEatEvent(Habbo habbo, Pet pet, HabboItem item) {
        super(habbo);

        this.habbo = habbo;
        this.pet = pet;
        this.item = item;
    }
}