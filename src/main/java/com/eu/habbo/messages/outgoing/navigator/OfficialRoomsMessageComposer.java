package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.navigation.DisplayMode;
import com.eu.habbo.habbohotel.navigation.NavigatorPublicCategory;
import com.eu.habbo.habbohotel.users.HabboNavigatorWindowSettings;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OfficialRoomsMessageComposer extends MessageComposer {
    private static final int TYPE_GUEST_ROOM = 2;
    private static final int TYPE_FOLDER = 4;
    private static final String OFFICIAL_ROOT_SEARCH_CODE = "official_view";

    private final List<NavigatorPublicCategory> publicCategories;
    private final HabboNavigatorWindowSettings navigatorWindowSettings;

    public OfficialRoomsMessageComposer(List<NavigatorPublicCategory> publicCategories, HabboNavigatorWindowSettings navigatorWindowSettings) {
        this.publicCategories = publicCategories;
        this.navigatorWindowSettings = navigatorWindowSettings;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.OfficialRoomsMessageComposer);

        List<OfficialRoomEntry> entries = new ArrayList<>();
        int index = 1;

        for (NavigatorPublicCategory category : this.publicCategories) {
            if (category == null || category.rooms.isEmpty()) {
                continue;
            }

            int categoryFolderIndex = index;
            entries.add(OfficialRoomEntry.folder(index++, 0, category.name, "", this.getFolderImageRef(category), this.isFolderOpen(category)));

            List<Room> rooms = new ArrayList<>(category.rooms);
            Collections.sort(rooms);

            for (Room room : rooms) {
                entries.add(OfficialRoomEntry.room(index++, categoryFolderIndex, room));
            }
        }

        this.response.appendInt(entries.size());
        for (OfficialRoomEntry entry : entries) {
            entry.serialize(this.response);
        }

        // Trailing payload reserved for staff-picked / promoted folders. Empty for now.
        this.response.appendInt(0);
        this.response.appendInt(0);
        return this.response;
    }

    private boolean isFolderOpen(NavigatorPublicCategory category) {
        if (this.navigatorWindowSettings == null) {
            return true;
        }

        String settingsKey = this.getSettingsKey(category);
        return this.navigatorWindowSettings.getDisplayModeForCategory(settingsKey, DisplayMode.VISIBLE) != DisplayMode.COLLAPSED;
    }

    private String getSettingsKey(NavigatorPublicCategory category) {
        if (category != null && category.id == Emulator.getGameEnvironment().getNavigatorManager().officialRootCategoryId) {
            return OFFICIAL_ROOT_SEARCH_CODE;
        }

        return category == null ? "" : category.name;
    }

    private String getFolderImageRef(NavigatorPublicCategory category) {
        if (category == null || category.imageUrl == null) {
            return "";
        }

        return category.imageUrl.trim();
    }

    private static abstract class OfficialRoomEntry {
        final int index;
        final int parentIndex;
        final String popupCaption;
        final String popupDescription;
        final String picRef;
        final int userCount;
        final int type;

        OfficialRoomEntry(int index, int parentIndex, String popupCaption, String popupDescription, String picRef, int userCount, int type) {
            this.index = index;
            this.parentIndex = parentIndex;
            this.popupCaption = popupCaption;
            this.popupDescription = popupDescription;
            this.picRef = picRef;
            this.userCount = userCount;
            this.type = type;
        }

        static OfficialRoomEntry folder(int index, int parentIndex, String caption, String description, String picRef, boolean open) {
            return new FolderEntry(index, parentIndex, caption, description, picRef, open);
        }

        static OfficialRoomEntry room(int index, int parentIndex, Room room) {
            return new RoomEntry(index, parentIndex, room);
        }

        final void serialize(ServerMessage message) {
            message.appendInt(this.index);
            message.appendString(this.popupCaption);
            message.appendString(this.popupDescription);
            message.appendInt(showDetails() ? 1 : 0);
            message.appendString(picText());
            message.appendString(this.picRef);
            message.appendInt(this.parentIndex);
            message.appendInt(this.userCount);
            message.appendInt(this.type);
            serializeTrailer(message);
        }

        abstract boolean showDetails();
        abstract String picText();
        abstract void serializeTrailer(ServerMessage message);
    }

    private static final class FolderEntry extends OfficialRoomEntry {
        private final boolean open;

        FolderEntry(int index, int parentIndex, String caption, String description, String picRef, boolean open) {
            super(index, parentIndex, caption, description, picRef, 0, TYPE_FOLDER);
            this.open = open;
        }

        @Override boolean showDetails() { return false; }
        @Override String picText() { return ""; }
        @Override void serializeTrailer(ServerMessage message) { message.appendBoolean(this.open); }
    }

    private static final class RoomEntry extends OfficialRoomEntry {
        private final Room room;

        RoomEntry(int index, int parentIndex, Room room) {
            super(index, parentIndex, room.getName(), room.getDescription(), "", room.getUserCount(), TYPE_GUEST_ROOM);
            this.room = room;
        }

        @Override boolean showDetails() { return true; }
        @Override String picText() { return ""; }
        @Override void serializeTrailer(ServerMessage message) { this.room.serialize(message); }
    }
}
