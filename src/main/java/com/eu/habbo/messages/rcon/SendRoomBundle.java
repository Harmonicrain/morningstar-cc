package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogPage;
import com.eu.habbo.habbohotel.catalog.layouts.RoomBundleLayout;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import com.google.gson.Gson;

public class SendRoomBundle extends RCONMessage<SendRoomBundle.JSON> {
    public SendRoomBundle() {
        super(JSON.class);
    }

    @Override
    public void handle(Gson gson, JSON json) {
        if (json.catalog_page > 0 && json.user_id > 0) {
            Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);
            CatalogPage page = Emulator.getGameEnvironment().getCatalogManager().getCatalogPage(json.catalog_page);

            if (!(page instanceof RoomBundleLayout)) {
                this.status = RCONMessage.STATUS_ERROR;
                this.message = "Catalog page is not a room bundle layout";
                return;
            }

            if (habbo != null) {
                ((RoomBundleLayout) page).buyRoom(habbo);
            } else {
                HabboInfo info = HabboManager.getOfflineHabboInfo(json.user_id);

                if (info != null) {
                    ((RoomBundleLayout) page).buyRoom(null, json.user_id, info.getUsername());
                } else {
                    this.status = RCONMessage.HABBO_NOT_FOUND;
                }
            }
        } else {
            this.status = RCONMessage.STATUS_ERROR;
            this.message = "Invalid catalog_page or user_id";
        }
    }

    static class JSON {

        public int user_id;


        public int catalog_page;
    }
}