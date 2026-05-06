package com.eu.habbo.messages.incoming.handshake;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.unknown.IgnoredUsersMessageComposer;

public class GetIgnoredUsersMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.packet != null && this.packet.bytesAvailable() > 0) {
            this.packet.readString();
        }

        this.client.sendResponse(new IgnoredUsersMessageComposer(this.client.getHabbo().getHabboStats().getIgnoredUsernames()));
    }
}
