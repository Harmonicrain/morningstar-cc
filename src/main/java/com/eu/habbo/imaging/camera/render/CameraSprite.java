package com.eu.habbo.imaging.camera.render;

public class CameraSprite implements Comparable<CameraSprite> {
    private String name;
    private int x;
    private int color = -1;
    private int y;
    private double z;
    private Integer alpha;
    private boolean flipH;
    private double skew;
    private String blendMode = "";
    private String paletteSourceName = "";

    public String getName() {
        return this.name;
    }

    public int getX() {
        return this.x;
    }

    public int getColor() {
        return this.color;
    }

    public int getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public Integer getAlpha() {
        return this.alpha;
    }

    public boolean isFlipH() {
        return this.flipH;
    }

    public String getBlendMode() {
        return this.blendMode;
    }

    public String getPaletteSourceName() {
        return this.paletteSourceName;
    }

    public boolean isFromUrl() {
        return this.name != null && this.name.contains("//");
    }

    public double getSkew() {
        return this.skew;
    }

    @Override
    public int compareTo(CameraSprite o) {
        int result = (o.getY() - this.getY());

        if (result == 0) {
            result = o.getX() - this.getX();
        }

        if (result == 0) {
            return Double.compare(o.getZ(), this.getZ());
        }

        return result;
    }
}
