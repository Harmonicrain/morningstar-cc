package com.eu.habbo.plugin.events.users.friends;

import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserFollowFriendEvent extends UserFriendEvent {

    public UserFollowFriendEvent(Habbo habbo, MessengerBuddy friend) {
        super(habbo, friend);
    }
}