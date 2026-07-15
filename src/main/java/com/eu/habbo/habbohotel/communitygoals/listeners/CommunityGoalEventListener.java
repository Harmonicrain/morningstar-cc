package com.eu.habbo.habbohotel.communitygoals.listeners;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.communitygoals.CommunityGoalTriggerType;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.plugin.events.users.catalog.UserEcotronRecycleEvent;
import com.eu.habbo.plugin.events.users.catalog.UserCatalogItemPurchasedEvent;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.List;

public class CommunityGoalEventListener {

    public static void onUserCatalogItemPurchasedEvent(UserCatalogItemPurchasedEvent event) {
        if (event.habbo == null || event.catalogItem == null || event.free || isCreditFurniture(event.catalogItem)) {
            return;
        }

        int userId = event.habbo.getHabboInfo().getId();
        int pageId = event.catalogItem.getPageId();
        int catalogItemId = event.catalogItem.getId();
        int pointsType = event.catalogItem.getPointsType();
        List<Integer> baseItemIds = getBaseItemIds(event.catalogItem);
        int itemCount = event.itemsList != null && !event.itemsList.isEmpty() ? event.itemsList.size() : event.amount;

        if (itemCount > 0) {
            Emulator.getGameEnvironment().getCommunityGoalManager().addTriggerContribution(userId, CommunityGoalTriggerType.CATALOGUE_PURCHASE, itemCount, pageId, catalogItemId, baseItemIds, -1);
        }

        int spentCredits = event.habbo.hasPermission(Permission.ACC_INFINITE_CREDITS) ? 0 : event.totalCredits;
        int spentPoints = event.habbo.hasPermission(Permission.ACC_INFINITE_POINTS) ? 0 : event.totalPoints;

        if (spentCredits > 0) {
            Emulator.getGameEnvironment().getCommunityGoalManager().addTriggerContribution(userId, CommunityGoalTriggerType.CREDITS_SPENT, spentCredits, pageId, catalogItemId, baseItemIds, -1);
        }

        if (spentPoints > 0) {
            Emulator.getGameEnvironment().getCommunityGoalManager().addTriggerContribution(userId, CommunityGoalTriggerType.POINTS_SPENT, spentPoints, pageId, catalogItemId, baseItemIds, pointsType);

            if (pointsType == 0) {
                Emulator.getGameEnvironment().getCommunityGoalManager().addTriggerContribution(userId, CommunityGoalTriggerType.DUCKETS_SPENT, spentPoints, pageId, catalogItemId, baseItemIds, pointsType);
            }
        }
    }

    public static void onUserEcotronRecycleEvent(UserEcotronRecycleEvent event) {
        if (event.habbo == null || event.items == null || event.items.isEmpty()) {
            return;
        }

        Emulator.getGameEnvironment().getCommunityGoalManager().addTriggerContribution(
                event.habbo.getHabboInfo().getId(),
                CommunityGoalTriggerType.ECOTRON_RECYCLE,
                event.items.size(),
                0,
                0,
                getBaseItemIds(event.items),
                -1);
    }

    private static List<Integer> getBaseItemIds(CatalogItem item) {
        List<Integer> baseItemIds = new ArrayList<>();

        if (item == null) {
            return baseItemIds;
        }

        for (Item baseItem : item.getBaseItems()) {
            if (baseItem != null) {
                baseItemIds.add(baseItem.getId());
            }
        }

        return baseItemIds;
    }

    private static List<Integer> getBaseItemIds(THashSet<HabboItem> items) {
        List<Integer> baseItemIds = new ArrayList<>();

        for (HabboItem item : items) {
            if (item != null && item.getBaseItem() != null) {
                baseItemIds.add(item.getBaseItem().getId());
            }
        }

        return baseItemIds;
    }

    private static boolean isCreditFurniture(CatalogItem catalogItem) {
        if (catalogItem == null) {
            return false;
        }

        if (isCreditFurnitureName(catalogItem.getName())) {
            return true;
        }

        for (Item item : catalogItem.getBaseItems()) {
            if (item != null && isCreditFurnitureName(item.getName())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isCreditFurnitureName(String name) {
        if (name == null) {
            return false;
        }

        String lowerName = name.toLowerCase();
        return lowerName.startsWith("cf_") || lowerName.startsWith("cfc_");
    }
}
