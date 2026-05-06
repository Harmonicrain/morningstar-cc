package com.eu.habbo.habbohotel.navigation;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.ArrayList;
import java.util.List;

public class NavigatorPublicFilter extends NavigatorFilter {
    public final static String name = "official_view";

    public NavigatorPublicFilter() {
        super(name);
    }

    @Override
    public List<SearchResultList> getResult(Habbo habbo) {
        boolean showInvisible = habbo.hasPermission(Permission.ACC_ENTERANYROOM) || habbo.hasPermission(Permission.ACC_ANYROOMOWNER);
        List<SearchResultList> resultLists = new ArrayList<>();

        int i = 0;
        for (NavigatorPublicCategory category : Emulator.getGameEnvironment().getNavigatorManager().publicCategories.values()) {
            if (!category.rooms.isEmpty()) {
                String settingsKey = getSettingsKey(category);
                resultLists.add(new SearchResultList(i, settingsKey, category.name, SearchAction.NONE, habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory(settingsKey, category.image), habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory(settingsKey), category.rooms, true, showInvisible, DisplayOrder.ORDER_NUM, category.order));
                i++;
            }
        }

        return resultLists;
    }

    private static String getSettingsKey(NavigatorPublicCategory category) {
        if (category != null && category.id == Emulator.getGameEnvironment().getNavigatorManager().officialRootCategoryId) {
            return name;
        }

        return category == null ? "" : category.name;
    }
}
