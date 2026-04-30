package com.eu.habbo.imaging.camera.render;

import com.eu.habbo.imaging.camera.CameraConstants;
import com.eu.habbo.imaging.camera.CameraPaletteCache;

import java.nio.file.Path;

public class CameraRenderRoom extends CameraRender {
    public CameraRenderRoom(JSONCamera result, int backgroundColor, Path spritesDir, CameraPaletteCache paletteCache, String wallPaint, WallColorResolver wallColorResolver) {
        super(result, CameraConstants.ROOM_RENDER_WIDTH, CameraConstants.ROOM_RENDER_HEIGHT, backgroundColor, spritesDir, paletteCache, wallPaint, wallColorResolver);
    }
}
