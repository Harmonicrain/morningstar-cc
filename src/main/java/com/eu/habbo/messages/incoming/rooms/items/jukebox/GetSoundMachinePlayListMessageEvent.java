package com.eu.habbo.messages.incoming.rooms.items.jukebox;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.TraxManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.rooms.items.jukebox.PlayListMessageComposer;

public class GetSoundMachinePlayListMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();

        if (room == null) {
            return;
        }

        TraxManager traxManager = room.getTraxManager();
        this.client.sendResponse(new PlayListMessageComposer(traxManager.soundTrackList(), traxManager.getSoundMachineSyncCountMs()));
    }
}
