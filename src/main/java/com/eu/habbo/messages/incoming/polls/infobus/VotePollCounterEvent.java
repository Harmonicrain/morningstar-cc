package com.eu.habbo.messages.incoming.polls.infobus;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.incoming.MessageHandler;

public class VotePollCounterEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int choiceIndex = this.packet.readInt();
        Habbo habbo = this.client.getHabbo();
        if (habbo == null || habbo.getRoomUnit() == null) return;
        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;
        Emulator.getGameEnvironment().getRoomPollManager().vote(room, habbo, choiceIndex);
    }
}
