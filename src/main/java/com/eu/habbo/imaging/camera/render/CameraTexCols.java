package com.eu.habbo.imaging.camera.render;

public class CameraTexCols {
    private String[] assetNames;

    public String[] getAssetNames() {
        return assetNames;
    }

    public String getAssetName(int index) {
        if (index > assetNames.length || index < 0)
            throw new IndexOutOfBoundsException();
        return assetNames[index];
    }
}
