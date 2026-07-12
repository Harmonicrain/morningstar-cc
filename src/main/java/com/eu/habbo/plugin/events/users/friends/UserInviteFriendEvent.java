package com.eu.habbo.plugin.events.users.friends;

import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.users.Habbo;

public class UserInviteFriendEvent extends UserFriendEvent {

    public UserInviteFriendEvent(Habbo habbo, MessengerBuddy friend) {
        super(habbo, friend);
    }

    public UserInviteFriendEvent(Habbo habbo) {
        super(habbo, null);
    }
}