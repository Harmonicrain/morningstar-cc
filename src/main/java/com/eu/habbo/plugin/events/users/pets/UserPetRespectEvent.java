package com.eu.habbo.plugin.events.users.pets;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public class UserPetRespectEvent extends UserEvent {
    public final Habbo habbo;
    public final Pet pet;

    public UserPetRespectEvent(Habbo habbo, Pet pet) {
        super(habbo);
        this.habbo = habbo;
        this.pet = pet;
    }
}