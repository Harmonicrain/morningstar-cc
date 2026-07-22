package com.eu.habbo.plugin.events.users;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;

public class UserPostItOtherUsersRoomEvent extends UserEvent {
    public final HabboItem postIt;
    public final Room room;

    public UserPostItOtherUsersRoomEvent(Habbo habbo, HabboItem postIt, Room room) {
        super(habbo);

        this.postIt = postIt;
        this.room = room;
    }
}
