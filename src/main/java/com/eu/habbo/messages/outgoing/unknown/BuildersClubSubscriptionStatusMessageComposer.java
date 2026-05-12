package com.eu.habbo.messages.outgoing.unknown;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.subscriptions.SubscriptionBuildersClub;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class BuildersClubSubscriptionStatusMessageComposer extends MessageComposer {
    private final Habbo habbo;

    public BuildersClubSubscriptionStatusMessageComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BuildersClubSubscriptionStatusMessageComposer);

        if (!SubscriptionBuildersClub.ENABLED) {
            // Builders Club is disabled hotel-wide: report an effectively-infinite
            // subscription so clients (e.g. the floorplan editor) don't gate features
            // like the floorplan save button behind a BC timer that will never tick.
            this.response.appendInt(Integer.MAX_VALUE);
            this.response.appendInt(SubscriptionBuildersClub.FURNI_LIMIT);
            this.response.appendInt(SubscriptionBuildersClub.MAX_FURNI_LIMIT);
            this.response.appendInt(Integer.MAX_VALUE);
            return this.response;
        }

        this.response.appendInt(this.habbo.getHabboStats().getBuildersClubSecondsRemaining());
        this.response.appendInt(this.habbo.getHabboStats().getBuildersClubFurniLimit());
        this.response.appendInt(this.habbo.getHabboStats().getBuildersClubMaxFurniLimit());
        this.response.appendInt(this.habbo.getHabboStats().getBuildersClubSecondsRemainingWithGrace());
        return this.response;
    }
}
