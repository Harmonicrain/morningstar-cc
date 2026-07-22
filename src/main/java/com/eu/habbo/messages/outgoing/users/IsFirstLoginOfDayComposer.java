package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class IsFirstLoginOfDayComposer extends MessageComposer {
    private final boolean firstLoginOfDay;

    public IsFirstLoginOfDayComposer(boolean firstLoginOfDay) {
        this.firstLoginOfDay = firstLoginOfDay;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.IsFirstLoginOfDayComposer);
        this.response.appendBoolean(this.firstLoginOfDay);
        return this.response;
    }
}
