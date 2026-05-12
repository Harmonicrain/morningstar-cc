package com.eu.habbo.messages.outgoing.polls.infobus;

import com.eu.habbo.habbohotel.polls.infobus.RoomPoll;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.List;

public class StartRoomPollMessageComposer extends MessageComposer {

    private final RoomPoll poll;

    public StartRoomPollMessageComposer(RoomPoll poll) {
        this.poll = poll;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.StartRoomPollMessageComposer);
        List<String> choices = poll.getChoices();
        this.response.appendString(poll.getQuestion());
        this.response.appendInt(choices.size());
        for (int i = 0; i < choices.size(); i++) {
            this.response.appendInt(i); // discarded by client parser
            this.response.appendString(choices.get(i));
        }
        return this.response;
    }
}
