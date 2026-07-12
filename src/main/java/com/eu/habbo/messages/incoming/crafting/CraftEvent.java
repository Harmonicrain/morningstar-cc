package com.eu.habbo.messages.incoming.crafting;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.crafting.CraftingAltar;
import com.eu.habbo.habbohotel.crafting.CraftingRecipe;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.catalog.LimitedEditionSoldOutMessageComposer;
import com.eu.habbo.messages.outgoing.crafting.CraftingResultMessageComposer;
import com.eu.habbo.messages.outgoing.inventory.UnseenItemsMessageComposer;
import com.eu.habbo.plugin.events.users.UserCraftProductEvent;
import com.eu.habbo.messages.outgoing.inventory.FurniListInvalidateMessageComposer;
import com.eu.habbo.messages.outgoing.inventory.FurniListRemoveMessageComposer;
import com.eu.habbo.threading.runnables.QueryDeleteHabboItems;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.Map;

public class CraftEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int craftingTable = this.packet.readInt();
        HabboItem item = this.client.getHabbo().getHabboInfo().getCurrentRoom().getHabboItem(craftingTable);
        CraftingAltar altar = Emulator.getGameEnvironment().getCraftingManager().getAltar(item.getBaseItem());
        CraftingRecipe recipe = altar.getRecipe(this.packet.readString());

        if (recipe != null) {
            if (!recipe.canBeCrafted()) {
                this.client.sendResponse(new LimitedEditionSoldOutMessageComposer());
                return;
            }

            TIntObjectHashMap<HabboItem> toRemove = new TIntObjectHashMap<>();
            for (Map.Entry<Item, Integer> set : recipe.getIngredients().entrySet()) {
                for (int i = 0; i < set.getValue(); i++) {
                    HabboItem habboItem = this.client.getHabbo().getInventory().getItemsComponent().getAndRemoveHabboItem(set.getKey());

                    if (habboItem == null) {
                        return;
                    }

                    toRemove.put(habboItem.getId(), habboItem);
                }
            }

            HabboItem rewardItem = Emulator.getGameEnvironment().getItemManager().createItem(this.client.getHabbo().getHabboInfo().getId(), recipe.getReward(), 0, 0, "");

            if (rewardItem != null) {
                if (recipe.isLimited()) {
                    recipe.decrease();
                }

                if (!recipe.getAchievement().isEmpty()) {
                    AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement(recipe.getAchievement()));

                }

                this.client.sendResponse(new CraftingResultMessageComposer(recipe));
                this.client.getHabbo().getInventory().getItemsComponent().addItem(rewardItem);
                this.client.sendResponse(new UnseenItemsMessageComposer(rewardItem));
                AchievementManager.progressAchievement(this.client.getHabbo(), Emulator.getGameEnvironment().getAchievementManager().getAchievement("Atcg"));
                toRemove.forEachValue(object -> {
                    CraftEvent.this.client.sendResponse(new FurniListRemoveMessageComposer(object.getGiftAdjustedId()));
                    return true;
                });
                this.client.sendResponse(new FurniListInvalidateMessageComposer());

                Emulator.getThreading().run(new QueryDeleteHabboItems(toRemove));
                Emulator.getPluginManager().fireEvent(new UserCraftProductEvent(this.client.getHabbo(), recipe.getReward()));
                return;
            }

        }

        this.client.sendResponse(new CraftingResultMessageComposer(null));
    }
}
