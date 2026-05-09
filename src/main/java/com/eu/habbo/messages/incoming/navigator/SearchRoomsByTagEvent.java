package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

public class SearchRoomsByTagEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        String tag = this.packet.readString();

        NavigatorMixedModeSearchHelper.send(this.client, Emulator.getGameEnvironment().getRoomManager().getRoomsWithTag(tag), "query", "tag:" + tag);
    }
}
