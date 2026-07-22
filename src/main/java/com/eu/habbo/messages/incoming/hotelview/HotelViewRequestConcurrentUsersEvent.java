package com.eu.habbo.messages.incoming.hotelview;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalManager;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.hotelview.ConcurrentUsersGoalProgressMessageComposer;

public class HotelViewRequestConcurrentUsersEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int userCount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
        CommunityGoalManager communityGoalManager = Emulator.getGameEnvironment().getCommunityGoalManager();
        int goal = communityGoalManager == null ? 0 : communityGoalManager.getConcurrentUsersGoalTarget();

        this.client.sendResponse(new ConcurrentUsersGoalProgressMessageComposer(ConcurrentUsersGoalProgressMessageComposer.ACTIVE, userCount, goal));
    }
}
