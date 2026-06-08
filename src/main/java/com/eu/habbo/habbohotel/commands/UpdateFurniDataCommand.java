package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.messages.outgoing.session.FurniDataReloadMessageComposer;

public class UpdateFurniDataCommand extends Command {
    public UpdateFurniDataCommand() {
        super("cmd_update_furnidata", Emulator.getTexts().getValue("commands.keys.cmd_update_furnidata").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        String reloadToken = String.valueOf(System.currentTimeMillis());
        Emulator.getGameServer().getGameClientManager().sendBroadcastResponse(new FurniDataReloadMessageComposer(reloadToken));

        gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.succes.cmd_update_furnidata"), RoomChatMessageBubbles.ALERT);

        return true;
    }
}
