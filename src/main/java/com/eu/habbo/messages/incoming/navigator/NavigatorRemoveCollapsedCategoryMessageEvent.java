package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.navigation.DisplayMode;
import com.eu.habbo.habbohotel.navigation.NavigatorPublicCategory;
import com.eu.habbo.habbohotel.users.HabboNavigatorWindowSettings;
import com.eu.habbo.messages.incoming.MessageHandler;

public class NavigatorRemoveCollapsedCategoryMessageEvent extends MessageHandler {
    private static final String OFFICIAL_ROOT_SEARCH_CODE = "official_view";

    @Override
    public void handle() throws Exception {
        String category = this.packet.readString();
        this.setDisplayMode(category, DisplayMode.VISIBLE);
    }

    private void setDisplayMode(String category, DisplayMode displayMode) {
        HabboNavigatorWindowSettings settings = this.client.getHabbo().getHabboStats().navigatorWindowSettings;
        settings.setDisplayMode(this.getSettingsKey(category), displayMode);
    }

    private String getSettingsKey(String category) {
        String officialRootCategoryName = this.getOfficialRootCategoryName();
        if (officialRootCategoryName != null && officialRootCategoryName.equals(category)) {
            return OFFICIAL_ROOT_SEARCH_CODE;
        }

        return category;
    }

    private String getOfficialRootCategoryName() {
        int officialRootCategoryId = Emulator.getGameEnvironment().getNavigatorManager().officialRootCategoryId;
        NavigatorPublicCategory category = Emulator.getGameEnvironment().getNavigatorManager().publicCategories.get(officialRootCategoryId);
        return category == null ? null : category.name;
    }
}
