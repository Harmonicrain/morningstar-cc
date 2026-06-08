package com.eu.habbo.messages.outgoing.session;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class FurniDataReloadMessageComposer extends MessageComposer {
    private final String reloadToken;

    public FurniDataReloadMessageComposer(String reloadToken) {
        this.reloadToken = reloadToken;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.FurniDataReloadMessageComposer);
        this.response.appendString(this.reloadToken);
        return this.response;
    }
}
