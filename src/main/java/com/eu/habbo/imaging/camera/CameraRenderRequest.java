package com.eu.habbo.imaging.camera;

public record CameraRenderRequest(
        int userId,
        String username,
        int roomId,
        int timestamp,
        int backgroundColor,
        String wallPaint,
        String json
) {
    public static CameraRenderRequest forPhoto(int userId, String username, int roomId, int timestamp, int backgroundColor, String wallPaint, String json) {
        return new CameraRenderRequest(userId, username, roomId, timestamp, backgroundColor, wallPaint, json);
    }

    public static CameraRenderRequest forThumbnail(int userId, String username, int roomId, int backgroundColor, String wallPaint, String json) {
        return new CameraRenderRequest(userId, username, roomId, 0, backgroundColor, wallPaint, json);
    }
}
