package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.navigation.NavigatorPublicCategory;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.navigator.OfficialRoomsMessageComposer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetOfficialRoomsMessageEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        // Client sends a category-id filter; ignored for now (we always return all public rooms).
        if (this.packet != null && this.packet.bytesAvailable() >= 4) {
            this.packet.readInt();
        }

        List<NavigatorPublicCategory> publicCategories = new ArrayList<>(Emulator.getGameEnvironment().getNavigatorManager().publicCategories.values());
        Collections.sort(publicCategories, (a, b) -> Integer.compare(a.order, b.order));
        this.client.sendResponse(new OfficialRoomsMessageComposer(publicCategories, this.client.getHabbo().getHabboStats().navigatorWindowSettings));
    }
}
