package com.eu.habbo.messages.incoming.catalog;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.unknown.SeasonalCalendarDailyOfferMessageComposer;

public class GetSeasonalCalendarDailyOfferEvent extends MessageHandler {

    @Override
    public void handle() throws Exception {
        int offerId = Emulator.getConfig().getInt("quests.seasonal.offer_id", 0);

        if (offerId <= 0)
            return;

        CatalogItem item = Emulator.getGameEnvironment().getCatalogManager().getCatalogItemByOfferId(offerId);

        if (item == null)
            return;

        this.client.sendResponse(new SeasonalCalendarDailyOfferMessageComposer(item.getPageId(), item));
    }
}
