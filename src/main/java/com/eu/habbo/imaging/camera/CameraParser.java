package com.eu.habbo.imaging.camera;

import com.eu.habbo.imaging.camera.render.JSONCamera;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CameraParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraParser.class);

    private final JSONCamera result;

    public CameraParser(String jsonData) {
        JSONCamera parsed = null;
        try {
            parsed = new GsonBuilder().create().fromJson(jsonData, JSONCamera.class);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse camera JSON", e);
        }

        if (parsed != null && parsed.getRoomid() < 0) {
            throw new IndexOutOfBoundsException("Room id is negative.");
        }

        this.result = parsed;
    }

    public JSONCamera getResult() {
        return this.result;
    }
}
