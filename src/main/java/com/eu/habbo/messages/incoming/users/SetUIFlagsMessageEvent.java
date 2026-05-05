package com.eu.habbo.messages.incoming.users;

import com.eu.habbo.habbohotel.users.HabboStats;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.users.PerkAllowancesMessageComposer;

public class SetUIFlagsMessageEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int flags = this.packet.readInt() & HabboStats.UI_FLAG_KNOWN_BITS;

        HabboStats stats = this.client.getHabbo().getHabboStats();
        boolean newNavigatorEnabled = (flags & HabboStats.UI_FLAG_NEW_NAVIGATOR) != 0;
        boolean newNavigatorChanged = stats.isNewNavigatorEnabled() != newNavigatorEnabled;

        stats.uiFlags = flags & ~HabboStats.UI_FLAG_NEW_NAVIGATOR;
        stats.setNewNavigatorEnabled(newNavigatorEnabled);
        stats.run();

        if (newNavigatorChanged) {
            this.client.sendResponse(new PerkAllowancesMessageComposer(this.client.getHabbo()));
        }
    }
}
