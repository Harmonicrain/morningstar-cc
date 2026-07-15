package com.eu.habbo.plugin.events.users.catalog;

import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.plugin.events.users.UserEvent;
import gnu.trove.set.hash.THashSet;

public class UserEcotronRecycleEvent extends UserEvent {
    public final THashSet<HabboItem> items;

    public UserEcotronRecycleEvent(Habbo habbo, THashSet<HabboItem> items) {
        super(habbo);

        this.items = items;
    }
}
