package com.eu.habbo.plugin.events.users.catalog;

import com.eu.habbo.habbohotel.catalog.CatalogItem;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.set.hash.THashSet;

import java.util.List;

public class UserCatalogItemPurchasedEvent extends UserCatalogEvent {

    public final THashSet<HabboItem> itemsList;


    public int totalCredits;


    public int totalPoints;


    public List<String> badges;
    public final int amount;
    public final boolean free;


    public UserCatalogItemPurchasedEvent(Habbo habbo, CatalogItem catalogItem, THashSet<HabboItem> itemsList, int totalCredits, int totalPoints, List<String> badges) {
        this(habbo, catalogItem, itemsList, totalCredits, totalPoints, badges, 1, false);
    }

    public UserCatalogItemPurchasedEvent(Habbo habbo, CatalogItem catalogItem, THashSet<HabboItem> itemsList, int totalCredits, int totalPoints, List<String> badges, int amount, boolean free) {
        super(habbo, catalogItem);

        this.itemsList = itemsList;
        this.totalCredits = totalCredits;
        this.totalPoints = totalPoints;
        this.badges = badges;
        this.amount = amount;
        this.free = free;
    }
}
