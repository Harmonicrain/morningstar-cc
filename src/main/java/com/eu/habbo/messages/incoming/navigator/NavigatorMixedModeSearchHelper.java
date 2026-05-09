package com.eu.habbo.messages.incoming.navigator;

import com.eu.habbo.habbohotel.navigation.*;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.navigator.GuestRoomSearchResultMessageComposer;
import com.eu.habbo.messages.outgoing.navigator.NavigatorSearchResultBlocksMessageComposer;
import com.eu.habbo.habbohotel.gameclients.GameClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NavigatorMixedModeSearchHelper {
    private NavigatorMixedModeSearchHelper() {
    }

    public static void send(GameClient client, List<Room> rooms, String searchCode, String searchQuery) {
        send(client, rooms, searchCode, searchQuery, true);
    }

    public static void send(GameClient client, List<Room> rooms, String searchCode, String searchQuery, boolean useLegacyTail) {
        if (client == null || client.getHabbo() == null) {
            return;
        }

        Habbo habbo = client.getHabbo();
        if (!habbo.getHabboStats().isNewNavigatorEnabled()) {
            client.sendResponse(new GuestRoomSearchResultMessageComposer(rooms, useLegacyTail));
            return;
        }

        String code = (searchCode == null || searchCode.isEmpty()) ? "query" : searchCode;
        String query = (searchQuery == null) ? "" : searchQuery;
        List<Room> roomResults = new ArrayList<>(rooms);

        SearchResultList list = new SearchResultList(
                0,
                code,
                query,
                SearchAction.NONE,
                habbo.getHabboStats().navigatorWindowSettings.getListModeForCategory(code, ListMode.LIST),
                habbo.getHabboStats().navigatorWindowSettings.getDisplayModeForCategory(code, DisplayMode.VISIBLE),
                roomResults,
                true,
                habbo.hasPermission(Permission.ACC_ENTERANYROOM) || habbo.hasPermission(Permission.ACC_ANYROOMOWNER),
                DisplayOrder.ACTIVITY,
                -1
        );

        client.sendResponse(new NavigatorSearchResultBlocksMessageComposer(code, query, Collections.singletonList(list)));
    }

    public static ServerMessage composeLegacy(List<Room> rooms) {
        return new GuestRoomSearchResultMessageComposer(rooms).compose();
    }
}
