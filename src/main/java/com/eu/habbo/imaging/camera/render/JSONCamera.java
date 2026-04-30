package com.eu.habbo.imaging.camera.render;

public class JSONCamera {
    private CameraPlane[] planes;
    private CameraSprite[] sprites;
    private CameraFilter[] filters;
    private int roomid;
    private int status;
    private float timestamp;
    private int checksum;

    public CameraPlane[] getPlanes() {
        return this.planes;
    }

    public CameraSprite[] getSprites() {
        return this.sprites;
    }

    public CameraFilter[] getFilters() {
        return this.filters;
    }

    public int getRoomid() {
        return this.roomid;
    }

    public int getStatus() {
        return this.status;
    }

    public float getTimestamp() {
        return this.timestamp;
    }

    public int getChecksum() {
        return this.checksum;
    }
}
