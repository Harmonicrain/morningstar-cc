package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.util.crypto.ZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderRoomThumbnailMessageEvent extends MessageHandler {
    private static final Logger CAMERA_LOGGER = LoggerFactory.getLogger("camera.capture");

    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission("acc_camera")) {
            this.client.getHabbo().alert(Emulator.getTexts().getValue("camera.permission"));
            return;
        }

        if (!this.client.getHabbo().getHabboInfo().getCurrentRoom().isOwner(this.client.getHabbo()))
            return;

        this.packet.getBuffer().readFloat();
        byte[] data = this.packet.getBuffer().readBytes(this.packet.getBuffer().readableBytes()).array();
        String content = new String(ZIP.inflate(data));

        int timestamp = Emulator.getIntUnixTimestamp();
        var currentRoom = this.client.getHabbo().getHabboInfo().getCurrentRoom();
        int backgroundColor = currentRoom.getBackgroundTonerColor().getRGB();
        String wallPaint = currentRoom.getWallPaint();
        int roomId = currentRoom.getId();
        String username = this.client.getHabbo().getHabboInfo().getUsername();
        int userId = this.client.getHabbo().getHabboInfo().getId();

        this.client.getHabbo().getHabboInfo().setPhotoJSON(Emulator.getConfig().getValue("camera.extradata").replace("%timestamp%", timestamp + ""));
        this.client.getHabbo().getHabboInfo().setPhotoTimestamp(timestamp);

        CAMERA_LOGGER.info("event=thumbnail_request user={} userId={} roomId={} timestamp={} json={}", username, userId, roomId, timestamp, content);

        Emulator.getCameraRenderManager().renderThumbnailAsync(this.client.getHabbo(), backgroundColor, wallPaint, content);
    }
}
