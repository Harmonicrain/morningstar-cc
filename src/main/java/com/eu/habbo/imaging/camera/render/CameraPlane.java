package com.eu.habbo.imaging.camera.render;

public class CameraPlane {
    private float z;
    private boolean bottomAligned;
    private int color;
    private int textureOffsetX;
    private int textureOffsetY;
    private CameraPositionPoint[] cornerPoints;
    private CameraTexCols[] texCols;
    private CameraMask[] masks;
    private CameraRectangleMask[] rectangleMasks;

    public float getZ() {
        return z;
    }

    public boolean isBottomAligned() {
        return bottomAligned;
    }

    public int getColor() {
        return color;
    }

    public int getTextureOffsetX() {
        return textureOffsetX;
    }

    public int getTextureOffsetY() {
        return textureOffsetY;
    }

    public CameraPositionPoint[] getCornerPoints() {
        return cornerPoints;
    }

    public CameraTexCols[] getTexCols() {
        return texCols;
    }

    public CameraMask[] getMasks() {
        return masks;
    }

    public CameraRectangleMask[] getRectangleMasks() {
        return rectangleMasks;
    }
}
