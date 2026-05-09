package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GuestRoomSearchResultMessageComposer extends MessageComposer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuestRoomSearchResultMessageComposer.class);
    private static final String CONFIG_AD_ENABLED = "navigator.legacy.search.ad.enabled";
    private static final String CONFIG_AD_ROOM_ID = "navigator.legacy.search.ad.room_id";
    private static final String CONFIG_AD_TITLE = "navigator.legacy.search.ad.title";
    private static final String CONFIG_AD_DESCRIPTION = "navigator.legacy.search.ad.description";
    private static final String CONFIG_AD_IMAGE = "navigator.legacy.search.ad.image";

    private final List<Room> rooms;
    private final boolean includeConfiguredAd;

    public GuestRoomSearchResultMessageComposer(List<Room> rooms) {
        this(rooms, true);
    }

    public GuestRoomSearchResultMessageComposer(List<Room> rooms, boolean includeConfiguredAd) {
        this.rooms = rooms;
        this.includeConfiguredAd = includeConfiguredAd;
    }

    @Override
    protected ServerMessage composeInternal() {
        try {
            this.response.init(Outgoing.GuestRoomSearchResultMessageComposer);

            this.response.appendInt(2);
            this.response.appendString("");

            this.response.appendInt(this.rooms.size());

            for (Room room : this.rooms) {
                room.serialize(this.response);
            }

            this.serializeConfiguredAd();

            return this.response;
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
        return null;
    }

    private void serializeConfiguredAd() {
        if (!this.includeConfiguredAd || !Emulator.getConfig().getBoolean(CONFIG_AD_ENABLED, false)) {
            this.response.appendBoolean(false);
            return;
        }

        int roomId = Emulator.getConfig().getInt(CONFIG_AD_ROOM_ID, 0);
        if (roomId <= 0) {
            this.response.appendBoolean(false);
            return;
        }

        Room room = Emulator.getGameEnvironment().getRoomManager().loadRoom(roomId);
        if (room == null) {
            this.response.appendBoolean(false);
            return;
        }

        this.response.appendBoolean(true);
        OfficialRoomEntryDataSerializer.serializeGuestRoom(
                this.response,
                0,
                0,
                room,
                Emulator.getConfig().getValue(CONFIG_AD_TITLE, ""),
                Emulator.getConfig().getValue(CONFIG_AD_DESCRIPTION, ""),
                this.getAdImageRef(roomId)
        );
    }

    private String getAdImageRef(int roomId) {
        String configuredImage = Emulator.getConfig().getValue(CONFIG_AD_IMAGE, "");
        if (configuredImage != null && !configuredImage.trim().isEmpty()) {
            return configuredImage;
        }

        String thumbnailUrl = Emulator.getConfig().getValue("camera.output.thumbnail.url", "");
        if (thumbnailUrl == null || thumbnailUrl.trim().isEmpty()) {
            return "";
        }

        thumbnailUrl = thumbnailUrl.trim();
        if (!thumbnailUrl.endsWith("/")) {
            thumbnailUrl += "/";
        }

        return thumbnailUrl + roomId + ".png";
    }

    public List<Room> getRooms() {
        return rooms;
    }
}
