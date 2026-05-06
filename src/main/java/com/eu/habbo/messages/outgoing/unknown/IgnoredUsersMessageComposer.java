package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.Collections;
import java.util.List;

public class IgnoredUsersMessageComposer extends MessageComposer {
    private final List<String> ignoredUsers;

    public IgnoredUsersMessageComposer() {
        this(Collections.emptyList());
    }

    public IgnoredUsersMessageComposer(List<String> ignoredUsers) {
        this.ignoredUsers = ignoredUsers;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.IgnoredUsersMessageComposer);

        this.response.appendInt(this.ignoredUsers.size());
        for (String ignoredUser : this.ignoredUsers) {
            this.response.appendString(ignoredUser);
        }

        return this.response;
    }
}
