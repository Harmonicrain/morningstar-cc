package com.eu.habbo.messages.outgoing.navigator;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.messages.ServerMessage;

public final class OfficialRoomEntryDataSerializer {
    public static final int TYPE_TAG = 1;
    public static final int TYPE_GUEST_ROOM = 2;
    public static final int TYPE_FOLDER = 4;

    private OfficialRoomEntryDataSerializer() {
    }

    public static void serializeGuestRoom(ServerMessage message, int index, int parentIndex, Room room, String title, String description, String imageRef) {
        serializeBase(
                message,
                index,
                parentIndex,
                valueOrDefault(title, room.getName()),
                valueOrDefault(description, room.getDescription()),
                "",
                valueOrDefault(imageRef, ""),
                room.getUserCount(),
                TYPE_GUEST_ROOM,
                true
        );
        room.serialize(message);
    }

    public static void serializeFolder(ServerMessage message, int index, int parentIndex, String title, String description, String imageRef, boolean open) {
        serializeBase(message, index, parentIndex, valueOrDefault(title, ""), valueOrDefault(description, ""), "", valueOrDefault(imageRef, ""), 0, TYPE_FOLDER, false);
        message.appendBoolean(open);
    }

    private static void serializeBase(ServerMessage message, int index, int parentIndex, String title, String description, String picText, String imageRef, int userCount, int type, boolean showDetails) {
        message.appendInt(index);
        message.appendString(title);
        message.appendString(description);
        message.appendInt(showDetails ? 1 : 0);
        message.appendString(picText);
        message.appendString(imageRef);
        message.appendInt(parentIndex);
        message.appendInt(userCount);
        message.appendInt(type);
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value.trim();
    }
}
