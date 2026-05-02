package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.imaging.camera.CameraRenderRequest;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.util.crypto.ZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class RenderRoomMessageEvent extends MessageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderRoomMessageEvent.class);

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission("acc_camera")) {
            this.client.getHabbo().alert(Emulator.getTexts().getValue("camera.permission"));
            return;
        }

        Room currentRoom = this.client.getHabbo().getHabboInfo().getCurrentRoom();
        if (currentRoom == null) {
            return;
        }

        this.packet.getBuffer().readFloat(); // consumes the 4-byte ByteArray length prefix from the wire protocol

        String username = this.client.getHabbo().getHabboInfo().getUsername();
        int userId = this.client.getHabbo().getHabboInfo().getId();

        int compressedLen = this.packet.getBuffer().readableBytes();
        int maxCompressed = Emulator.getConfig().getInt("camera.limits.compressed.bytes", 16384);
        if (compressedLen > maxCompressed) {
            LOGGER.warn("Photo request rejected (compressed payload too large): user={} userId={} size={} limit={}", username, userId, compressedLen, maxCompressed);
            this.client.getHabbo().alert(Emulator.getTexts().getValue("camera.error.creation"));
            return;
        }

        byte[] data = this.packet.getBuffer().readBytes(compressedLen).array();

        int maxInflated = Emulator.getConfig().getInt("camera.limits.inflated.bytes", 262144);
        byte[] inflated;
        try {
            inflated = ZIP.inflate(data, maxInflated);
        } catch (IOException e) {
            LOGGER.warn("Photo request rejected (inflate overflow): user={} userId={} compressedLen={} limit={} detail={}", username, userId, compressedLen, maxInflated, e.getMessage());
            this.client.getHabbo().alert(Emulator.getTexts().getValue("camera.error.creation"));
            return;
        }
        String content = new String(inflated, StandardCharsets.UTF_8);

        int timestamp = Emulator.getIntUnixTimestamp();
        int backgroundColor = currentRoom.getBackgroundTonerColor().getRGB();
        String wallPaint = currentRoom.getWallPaint();
        int roomId = currentRoom.getId();

        this.client.getHabbo().getHabboInfo().setPhotoJSON(Emulator.getConfig().getValue("camera.extradata").replace("%timestamp%", timestamp + ""));
        this.client.getHabbo().getHabboInfo().setPhotoTimestamp(timestamp);
        this.client.getHabbo().getHabboInfo().setPhotoRoomId(roomId);

        Emulator.getCameraRenderManager().renderPhotoAsync(
                CameraRenderRequest.forPhoto(userId, username, roomId, timestamp, backgroundColor, wallPaint, content));
    }
}
