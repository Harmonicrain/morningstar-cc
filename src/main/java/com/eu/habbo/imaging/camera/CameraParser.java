package com.eu.habbo.imaging.camera;

import com.eu.habbo.Emulator;
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

        if (parsed != null) {
            int maxPlanes = Emulator.getConfig().getInt("camera.limits.planes", 64);
            int maxSprites = Emulator.getConfig().getInt("camera.limits.sprites", 512);
            int maxFilters = Emulator.getConfig().getInt("camera.limits.filters", 16);

            if (parsed.getPlanes() != null && parsed.getPlanes().length > maxPlanes) {
                throw new IllegalArgumentException("planes exceeds limit: " + parsed.getPlanes().length + " > " + maxPlanes);
            }
            if (parsed.getSprites() != null && parsed.getSprites().length > maxSprites) {
                throw new IllegalArgumentException("sprites exceeds limit: " + parsed.getSprites().length + " > " + maxSprites);
            }
            if (parsed.getFilters() != null && parsed.getFilters().length > maxFilters) {
                throw new IllegalArgumentException("filters exceeds limit: " + parsed.getFilters().length + " > " + maxFilters);
            }
        }

        this.result = parsed;
    }

    public JSONCamera getResult() {
        return this.result;
    }
}
