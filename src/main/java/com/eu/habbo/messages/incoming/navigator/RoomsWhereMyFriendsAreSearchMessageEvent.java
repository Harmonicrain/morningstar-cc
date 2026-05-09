package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RoomsWhereMyFriendsAreSearchMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        NavigatorMixedModeSearchHelper.send(this.client, Emulator.getGameEnvironment().getRoomManager().getRoomsFriendsNow(this.client.getHabbo()), "with_friends", "");
    }
}
