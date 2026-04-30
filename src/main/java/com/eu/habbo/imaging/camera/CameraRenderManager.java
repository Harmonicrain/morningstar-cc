package com.eu.habbo.imaging.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.achievements.AchievementManager;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.imaging.camera.render.CameraRender;
import com.eu.habbo.imaging.camera.render.CameraRenderImage;
import com.eu.habbo.imaging.camera.render.CameraRenderRoom;
import com.eu.habbo.imaging.camera.render.CameraUtils;
import com.eu.habbo.imaging.camera.render.JSONCamera;
import com.eu.habbo.imaging.camera.render.WallColorResolver;
import com.eu.habbo.messages.outgoing.camera.CameraStorageUrlMessageComposer;
import com.eu.habbo.messages.outgoing.camera.ThumbnailStatusMessageComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CameraRenderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraRenderManager.class);
    private static final Logger CAMERA_LOGGER = LoggerFactory.getLogger("camera.capture");

    private final Path spritesDir;
    private final Path binaryDir;
    private final Path outputDir;
    private final Path thumbnailOutputDir;
    private final String urlPrefix;
    private final String thumbnailUrlPrefix;
    private final CameraPaletteCache paletteCache;
    private final WallColorResolver wallColorResolver;

    public CameraRenderManager() {
        this.spritesDir = Paths.get(Emulator.getConfig().getValue("camera.assets.sprites.path")).toAbsolutePath().normalize();
        this.binaryDir = Paths.get(Emulator.getConfig().getValue("camera.assets.binary.path")).toAbsolutePath().normalize();
        this.outputDir = Paths.get(Emulator.getConfig().getValue("camera.output.path")).toAbsolutePath().normalize();
        this.thumbnailOutputDir = Paths.get(Emulator.getConfig().getValue("camera.output.thumbnail.path")).toAbsolutePath().normalize();

        String configuredUrl = Emulator.getConfig().getValue("camera.output.url");
        if (!configuredUrl.endsWith("/")) {
            configuredUrl = configuredUrl + "/";
        }
        this.urlPrefix = configuredUrl;

        String configuredThumbnailUrl = Emulator.getConfig().getValue("camera.output.thumbnail.url");
        if (!configuredThumbnailUrl.endsWith("/")) {
            configuredThumbnailUrl = configuredThumbnailUrl + "/";
        }
        this.thumbnailUrlPrefix = configuredThumbnailUrl;

        try {
            Files.createDirectories(this.outputDir);
            Files.createDirectories(this.thumbnailOutputDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create camera output directories at {} and {}", this.outputDir, this.thumbnailOutputDir, e);
        }

        if (!Files.isDirectory(this.spritesDir)) {
            LOGGER.warn("Camera sprites directory does not exist: {} (renders will be missing textures and decorations)", this.spritesDir);
        }

        this.paletteCache = new CameraPaletteCache(this.binaryDir);

        Path wallColorsPath = Paths.get(Emulator.getConfig().getValue("camera.wall.colors.path", "./camera_wall_colors.properties")).toAbsolutePath().normalize();
        this.wallColorResolver = new WallColorResolver(wallColorsPath);

        LOGGER.info("Camera renderer initialised. sprites={}, binary={}, output={}, thumbnailOutput={}, url={}, thumbnailUrl={}",
                this.spritesDir, this.binaryDir, this.outputDir, this.thumbnailOutputDir, this.urlPrefix, this.thumbnailUrlPrefix);
    }

    public void renderPhotoAsync(Habbo habbo, int backgroundColor, String wallPaint, String json, int timestamp) {
        Emulator.getThreading().run(() -> renderPhoto(habbo, backgroundColor, wallPaint, json, timestamp));
    }

    public void renderThumbnailAsync(Habbo habbo, int backgroundColor, String wallPaint, String json) {
        Emulator.getThreading().run(() -> renderThumbnail(habbo, backgroundColor, wallPaint, json));
    }

    private void renderPhoto(Habbo habbo, int backgroundColor, String wallPaint, String json, int timestamp) {
        if (habbo == null || habbo.getClient() == null) {
            return;
        }

        String username = habbo.getHabboInfo().getUsername();
        int userId = habbo.getHabboInfo().getId();
        int roomId = habbo.getHabboInfo().getPhotoRoomId();

        try {
            CameraParser parser = new CameraParser(json);
            JSONCamera scene = parser.getResult();

            if (scene == null) {
                CAMERA_LOGGER.error("event=photo_failed user={} userId={} roomId={} timestamp={} reason=scene_parse_failed", username, userId, roomId, timestamp);
                handleRenderError(habbo);
                return;
            }

            CameraRender render = new CameraRenderImage(scene, backgroundColor, this.spritesDir, this.paletteCache, wallPaint, this.wallColorResolver);
            BufferedImage image = render.render();

            roomId = scene.getRoomid();

            String relativePath = username + "/" + userId + "_" + timestamp + ".png";
            saveImage(image, relativePath);

            String smallRelativePath = username + "/" + userId + "_" + timestamp + "_small.png";
            BufferedImage smallImage = CameraUtils.resize(image, 100, 100);
            saveImage(smallImage, smallRelativePath);

            String url = composeUrl(relativePath);

            CAMERA_LOGGER.info("event=photo_rendered user={} userId={} roomId={} timestamp={} file={} smallFile={} url={}",
                    username, userId, roomId, timestamp, relativePath, smallRelativePath, url);

            if (timestamp == habbo.getHabboInfo().getPhotoTimestamp()) {
                AchievementManager.progressAchievement(habbo, Emulator.getGameEnvironment().getAchievementManager().getAchievement("CameraPhotoCount"), 1);
                habbo.getClient().sendResponse(new CameraStorageUrlMessageComposer(url));
                habbo.getHabboInfo().setPhotoJSON(habbo.getHabboInfo().getPhotoJSON().replace("%room_id%", roomId + "").replace("%url%", url));
                habbo.getHabboInfo().setPhotoURL(url);
            }
        } catch (Exception e) {
            CAMERA_LOGGER.error("event=photo_failed user={} userId={} roomId={} timestamp={} reason={}",
                    username, userId, roomId, timestamp, e.getClass().getSimpleName(), e);
            LOGGER.error("Failed to render camera photo for {}", habbo.getHabboInfo().getUsername(), e);
            handleRenderError(habbo);
        }
    }

    private void renderThumbnail(Habbo habbo, int backgroundColor, String wallPaint, String json) {
        if (habbo == null || habbo.getClient() == null) {
            return;
        }

        String username = habbo.getHabboInfo().getUsername();
        int userId = habbo.getHabboInfo().getId();
        int roomId = habbo.getHabboInfo().getCurrentRoom() != null ? habbo.getHabboInfo().getCurrentRoom().getId() : 0;

        try {
            CameraParser parser = new CameraParser(json);
            JSONCamera scene = parser.getResult();

            if (scene == null) {
                CAMERA_LOGGER.error("event=thumbnail_failed user={} userId={} roomId={} reason=scene_parse_failed", username, userId, roomId);
                habbo.getClient().sendResponse(new ThumbnailStatusMessageComposer());
                return;
            }

            CameraRender render = new CameraRenderRoom(scene, backgroundColor, this.spritesDir, this.paletteCache, wallPaint, this.wallColorResolver);
            BufferedImage image = render.render();

            roomId = scene.getRoomid();

            String relativePath = roomId + ".png";
            saveImage(image, this.thumbnailOutputDir, relativePath);

            CAMERA_LOGGER.info("event=thumbnail_rendered user={} userId={} roomId={} file={} url={}",
                    username, userId, roomId, relativePath, composeThumbnailUrl(relativePath));

            habbo.getClient().sendResponse(new ThumbnailStatusMessageComposer());
        } catch (Exception e) {
            CAMERA_LOGGER.error("event=thumbnail_failed user={} userId={} roomId={} reason={}", username, userId, roomId, e.getClass().getSimpleName(), e);
            LOGGER.error("Failed to render camera thumbnail for {}", habbo.getHabboInfo().getUsername(), e);
            habbo.getClient().sendResponse(new ThumbnailStatusMessageComposer());
        }
    }

    private void handleRenderError(Habbo habbo) {
        habbo.getHabboInfo().setPhotoTimestamp(0);
        habbo.getHabboInfo().setPhotoJSON("");
        habbo.getHabboInfo().setPhotoURL("");
        habbo.alert(Emulator.getTexts().getValue("camera.error.creation"));
    }

    private void saveImage(BufferedImage image, String relativePath) throws IOException {
        saveImage(image, this.outputDir, relativePath);
    }

    private void saveImage(BufferedImage image, Path baseDir, String relativePath) throws IOException {
        File file = baseDir.resolve(relativePath).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Failed to create directory: " + parent.getAbsolutePath());
        }
        ImageIO.write(image, "png", file);
    }

    private String composeUrl(String relativePath) {
        String url = this.urlPrefix + relativePath.replace("\\", "/");
        if (!Emulator.getConfig().getBoolean("camera.use.https", true)) {
            url = url.replace("https://", "http://");
        }
        return url;
    }

    private String composeThumbnailUrl(String relativePath) {
        return this.thumbnailUrlPrefix + relativePath.replace("\\", "/");
    }
}
