package com.eu.habbo.messages.outgoing.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;
import java.util.List;

public class PerkAllowancesMessageComposer extends MessageComposer {
    private final Habbo habbo;

    public PerkAllowancesMessageComposer(Habbo habbo) {
        this.habbo = habbo;
    }

    @Override
    protected ServerMessage composeInternal() {
        boolean newNavigatorEnabled = this.habbo.getHabboStats().isNewNavigatorEnabled();

        List<Perk> perks = new ArrayList<>();
        perks.add(new Perk("USE_GUIDE_TOOL", "requirement.unfulfilled.helper_level_4", Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, Permission.ACC_HELPER_USE_GUIDE_TOOL)));
        perks.add(new Perk("GIVE_GUIDE_TOURS", "", Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, "acc_helper_give_guide_tours")));
        perks.add(new Perk("JUDGE_CHAT_REVIEWS", "requirement.unfulfilled.helper_level_6", Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, "acc_helper_judge_chat_reviews")));
        perks.add(new Perk("VOTE_IN_COMPETITIONS", "requirement.unfulfilled.helper_level_2", true));
        perks.add(new Perk("CALL_ON_HELPERS", "", true));
        perks.add(new Perk("CITIZEN", "", true));
        perks.add(new Perk("TRADE", "requirement.unfulfilled.no_trade_lock", this.habbo.getHabboStats().allowTrade()));
        perks.add(new Perk("HEIGHTMAP_EDITOR_BETA", "requirement.unfulfilled.feature_disabled", Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, Permission.ACC_FLOORPLAN_EDITOR)));
        perks.add(new Perk("BUILDER_AT_WORK", "", this.habbo.getHabboStats().hasEffectiveBuildersClub()));
        perks.add(new Perk("CAMERA", "", Emulator.getGameEnvironment().getPermissionsManager().hasPermission(this.habbo, "acc_camera")));
        // Mutually exclusive: PHASE_ONE drives the legacy navigator UI, PHASE_TWO drives the new one.
        // (Wire identifiers are frozen 2014 protocol strings — do not rename.)
        perks.add(new Perk("NAVIGATOR_PHASE_ONE_2014", "", !newNavigatorEnabled));
        perks.add(new Perk("NAVIGATOR_PHASE_TWO_2014", "", newNavigatorEnabled));
        perks.add(new Perk("MOUSE_ZOOM", "", true));
        perks.add(new Perk("NAVIGATOR_ROOM_THUMBNAIL_CAMERA", "", true));
        perks.add(new Perk("HABBO_CLUB_OFFER_BETA", "", true));

        this.response.init(Outgoing.PerkAllowancesMessageComposer);
        this.response.appendInt(perks.size());
        for (Perk perk : perks) {
            this.response.appendString(perk.code);
            this.response.appendString(perk.unmetReason);
            this.response.appendBoolean(perk.allowed);
        }
        return this.response;
    }

    public Habbo getHabbo() {
        return habbo;
    }

    private static final class Perk {
        final String code;
        final String unmetReason;
        final boolean allowed;

        Perk(String code, String unmetReason, boolean allowed) {
            this.code = code;
            this.unmetReason = unmetReason;
            this.allowed = allowed;
        }
    }
}
