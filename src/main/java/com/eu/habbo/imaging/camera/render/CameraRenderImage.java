package com.eu.habbo.imaging.camera.render;

import com.eu.habbo.imaging.camera.CameraConstants;
import com.eu.habbo.imaging.camera.CameraPaletteCache;

import java.nio.file.Path;

public class CameraRenderImage extends CameraRender {
    public CameraRenderImage(JSONCamera result, int backgroundColor, Path spritesDir, Path framesDir, CameraPaletteCache paletteCache, String wallPaint, WallColorResolver wallColorResolver) {
        super(result, CameraConstants.RENDER_WIDTH, CameraConstants.RENDER_HEIGHT, backgroundColor, spritesDir, framesDir, paletteCache, wallPaint, wallColorResolver);
    }
}
