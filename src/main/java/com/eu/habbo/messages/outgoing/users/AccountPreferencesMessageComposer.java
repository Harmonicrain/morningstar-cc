package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboStats;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class AccountPreferencesMessageComposer extends MessageComposer {
    private final Habbo habbo;

    public AccountPreferencesMessageComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        HabboStats stats = this.habbo.getHabboStats();
        int uiFlags = stats.uiFlags & ~HabboStats.UI_FLAG_NEW_NAVIGATOR;
        if (stats.isNewNavigatorEnabled()) {
            uiFlags |= HabboStats.UI_FLAG_NEW_NAVIGATOR;
        }

        this.response.init(Outgoing.AccountPreferencesMessageComposer);
        this.response.appendInt(stats.volumeSystem);
        this.response.appendInt(stats.volumeFurni);
        this.response.appendInt(stats.volumeTrax);
        this.response.appendBoolean(stats.preferOldChat);
        this.response.appendBoolean(stats.blockRoomInvites);
        this.response.appendBoolean(stats.blockCameraFollow);
        this.response.appendInt(uiFlags);
        this.response.appendInt(stats.chatColor.getType());
        return this.response;
    }

    public Habbo getHabbo() {
        return habbo;
    }
}
