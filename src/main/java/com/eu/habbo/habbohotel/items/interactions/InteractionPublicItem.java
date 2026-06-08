package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.items.Item;

/**
 * Synthetic, non-persistent floor item representing a public-room object (e.g. Net Cafe furniture)
 * for the purposes of collision / sit / stack. These are injected into a room's item manager from
 * the public_items table (see RoomItemManager.injectPublicItems) and never touch the `items` table.
 *
 * Persistence is hard-disabled: needsUpdate()/run() are no-ops so a stray needsUpdate(true) (from
 * onClick / moveFurniTo / etc.) can never produce an UPDATE/DELETE against a negative synthetic id.
 */
public class InteractionPublicItem extends InteractionDefault {

    public InteractionPublicItem(int id, Item baseItem) {
        super(id, 0, baseItem, "0", 0, 0);
    }

    @Override
    public boolean isPublicSpaceObject() {
        return true;
    }

    @Override
    public void needsUpdate(boolean value) {
        // no-op: synthetic public items are never persisted
    }

    @Override
    public boolean needsUpdate() {
        return false;
    }

    @Override
    public void run() {
        // no-op: never touch the database
    }
}
