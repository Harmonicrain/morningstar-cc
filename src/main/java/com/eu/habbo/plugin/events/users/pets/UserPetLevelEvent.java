package com.eu.habbo.plugin.events.users.pets;

import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.events.users.UserEvent;

public class UserPetLevelEvent extends UserEvent {
    public final Habbo habbo;
    public final Pet pet;
    public final int level;

    public UserPetLevelEvent(Habbo habbo, Pet pet, int level) {
        super(habbo);
        this.habbo = habbo;
        this.pet = pet;
        this.level = level;
    }
}