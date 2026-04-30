package com.eu.habbo.imaging.camera.render;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.ConfigurationManager;
import com.eu.habbo.threading.ThreadPooling;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CameraRenderTest {
    private static final int BLUE_BACKGROUND = 0x8BDFEF;
    private static final int BLUE_TONED_SIDE_WALL = 0x6FB2BF;

    @TempDir
    Path spritesDir;

    @BeforeAll
    static void installEmulatorConfig() throws Exception {
        Path configFile = Files.createTempFile("camera-render-test", ".ini");
        Files.writeString(configFile, """
                camera.image.fetch.max.per.render=30
                camera.image.fetch.budget.ms=8000
                camera.image.fetch.connect.timeout.ms=2000
                camera.image.fetch.read.timeout.ms=3000
                camera.image.fetch.max.bytes=2097152
                camera.allowed.image.hosts=
                """);

        Field configField = Emulator.class.getDeclaredField("config");
        configField.setAccessible(true);
        configField.set(null, new ConfigurationManager(configFile.toString()));

        Field threadingField = Emulator.class.getDeclaredField("threading");
        threadingField.setAccessible(true);
        threadingField.set(null, new ThreadPooling(1));
    }

    @Test
    void spriteLoaderRejectsPathTraversalAssets() {
        CameraImageLoader imageLoader = new CameraImageLoader(spritesDir);

        assertNull(imageLoader.spriteFile("../escape"));
        assertNull(imageLoader.readSprite("../escape"));
    }

    @Test
    void rendersTexturedWallUsingPlaneColor() throws Exception {
        writeSolidSprite("wall_texture_64_3_wall_color_jagged3", 32, 16, 0xFFFFFF);

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": %d,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":20,"y":50},{"x":180,"y":130},{"x":20,"y":230},{"x":180,"y":310}],
                      "texCols": [{"assetNames": ["wall_texture_64_3_wall_color_jagged3"]}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """.formatted(BLUE_BACKGROUND, BLUE_TONED_SIDE_WALL));

        assertRgb(BLUE_TONED_SIDE_WALL, image.getRGB(80, 160));
    }

    @Test
    void keepsPlaneColorWhenWallTextureAssetIsMissing() {
        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": %d,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":20,"y":50},{"x":180,"y":130},{"x":20,"y":230},{"x":180,"y":310}],
                      "texCols": [{"assetNames": ["wall_texture_64_0_missing"]}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """.formatted(BLUE_BACKGROUND, BLUE_TONED_SIDE_WALL));

        assertRgb(BLUE_TONED_SIDE_WALL, image.getRGB(80, 160));
    }

    @Test
    void wallPaintOverrideReplacesTexturedWallPlaneColor() throws Exception {
        writeSolidSprite("wall_texture_64_3_wall_color_jagged3", 32, 16, 0xFFFFFF);
        Path overrideFile = spritesDir.resolve("camera_wall_colors.properties");
        Files.writeString(overrideFile, "101=0x8844CC\n");

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 7320255,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":20,"y":50},{"x":180,"y":130},{"x":20,"y":230},{"x":180,"y":310}],
                      "texCols": [{"assetNames": ["wall_texture_64_3_wall_color_jagged3"]}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """.formatted(BLUE_BACKGROUND), "101", new WallColorResolver(overrideFile));

        assertRgb(0x8844CC, image.getRGB(80, 160));
    }

    @Test
    void rectangleMasksRevealPreviouslyRenderedPixels() throws Exception {
        writeSolidSprite("wall_texture_test_white", 10, 10, 0xFFFFFF);
        int background = 0x334422;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16711680,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}],
                      "rectangleMasks": [{"x":0,"y":0,"width":20,"height":20}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """.formatted(background));

        assertRgb(0xFF0000, image.getRGB(50, 70));
        assertRgb(background, image.getRGB(30, 90));
    }

    @Test
    void maskedWallDoorwayRestoresEarlierFloorTileUnderRevealedAvatar() throws Exception {
        writeSolidSprite("wall_texture_test_white", 10, 10, 0xFFFFFF);
        writeSolidSprite("avatar_test_green", 10, 40, 0x00FF00);
        writeSolidSprite("floor_texture_test_yellow", 10, 10, 0xFFF0AA);
        int background = 0x334422;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 0,
                      "color": 16777215,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["floor_texture_test_yellow"]}]
                    },
                    {
                      "z": 1,
                      "color": 16711680,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}],
                      "rectangleMasks": [{"x":0,"y":0,"width":20,"height":20}]
                    }
                  ],
                  "sprites": [
                    {"name": "avatar_test_green", "x": 20, "y": 60, "z": 0}
                  ],
                  "filters": []
                }
                """.formatted(background));

        assertRgb(0xFF0000, image.getRGB(50, 70));
        assertRgb(0x00FF00, image.getRGB(25, 90));
        assertRgb(0xFFF0AA, image.getRGB(35, 90));
    }

    @Test
    void bitmapWallMaskRevealsOnlyMaskedAvatarPixels() throws Exception {
        writeSolidSprite("wall_texture_test_white", 10, 10, 0xFFFFFF);
        writeSolidSprite("avatar_test_green", 20, 40, 0x00FF00);
        writeHalfAlphaSprite("door_mask_half", 20, 20);
        int background = 0x334422;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16711680,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}],
                      "masks": [{"name": "door_mask_half", "location": {"x":0,"y":0}, "flipH": false, "flipV": false}]
                    }
                  ],
                  "sprites": [
                    {"name": "avatar_test_green", "x": 20, "y": 60, "z": 0}
                  ],
                  "filters": []
                }
                """.formatted(background));

        assertRgb(0xFF0000, image.getRGB(50, 70));
        assertRgb(0x00FF00, image.getRGB(25, 90));
        assertRgb(0xFF0000, image.getRGB(35, 90));
    }

    @Test
    void foregroundFurnitureSpriteRemainsVisibleInDoorCutout() throws Exception {
        writeSolidSprite("wall_texture_test_white", 10, 10, 0xFFFFFF);
        writeSolidSprite("furni_test_green", 20, 40, 0x00FF00);
        int background = 0x334422;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16711680,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}],
                      "rectangleMasks": [{"x":0,"y":0,"width":20,"height":20}]
                    }
                  ],
                  "sprites": [
                    {"name": "furni_test_green", "x": 20, "y": 60, "z": 0}
                  ],
                  "filters": []
                }
                """.formatted(background));

        assertRgb(0xFF0000, image.getRGB(50, 70));
        assertRgb(0x00FF00, image.getRGB(30, 90));
    }

    @Test
    void doorwayAvatarDoesNotLeakOntoWallOutsideMask() throws Exception {
        writeSolidSprite("wall_texture_test_white", 10, 10, 0xFFFFFF);
        writeSolidSprite("avatar_test_green_wide", 30, 40, 0x00FF00);
        int background = 0x334422;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16711680,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}],
                      "rectangleMasks": [{"x":0,"y":0,"width":20,"height":20}]
                    }
                  ],
                  "sprites": [
                    {"name": "avatar_test_green_wide", "x": 20, "y": 60, "z": 0}
                  ],
                  "filters": []
                }
                """.formatted(background));

        assertRgb(0x00FF00, image.getRGB(25, 90));
        assertRgb(0xFF0000, image.getRGB(45, 90));
    }

    @Test
    void avatarNearDoorButOutsideOpeningIsCoveredByWall() throws Exception {
        writeSolidSprite("wall_texture_test_white", 10, 10, 0xFFFFFF);
        writeSolidSprite("h_std_door_green", 20, 40, 0x00FF00);
        writeSolidSprite("h_std_front_blue", 20, 40, 0x0000FF);
        int background = 0x334422;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16711680,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}],
                      "rectangleMasks": [{"x":0,"y":0,"width":20,"height":40}]
                    }
                  ],
                  "sprites": [
                    {"name": "avatar_0", "x": 20, "y": 60, "z": 0},
                    {"name": "h_std_door_green", "x": 20, "y": 60, "z": 0},
                    {"name": "avatar_1", "x": 40, "y": 60, "z": 0},
                    {"name": "h_std_front_blue", "x": 40, "y": 60, "z": 0}
                  ],
                  "filters": []
                }
                """.formatted(background));

        assertRgb(0x00FF00, image.getRGB(25, 90));
        assertRgb(0xFF0000, image.getRGB(45, 90));
    }

    @Test
    void frontAvatarClusterOutsideOpeningIsCoveredWhenShadowsPrecedeAnchors() throws Exception {
        writeSolidSprite("wall_texture_test_white", 10, 10, 0xFFFFFF);
        writeSolidSprite("h_std_shadow_back", 20, 8, 0x666666);
        writeSolidSprite("h_std_shadow_front", 20, 8, 0x444444);
        writeSolidSprite("h_std_door_green", 20, 40, 0x00FF00);
        writeSolidSprite("h_std_front_blue", 20, 40, 0x0000FF);
        int background = 0x334422;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16711680,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}],
                      "rectangleMasks": [{"x":0,"y":0,"width":20,"height":40}]
                    }
                  ],
                  "sprites": [
                    {"name": "h_std_shadow_back", "x": 20, "y": 92, "z": 0.66},
                    {"name": "h_std_shadow_front", "x": 35, "y": 92, "z": -0.04},
                    {"name": "avatar_1", "x": 20, "y": 60, "z": -0.35},
                    {"name": "h_std_door_green", "x": 20, "y": 60, "z": -0.35},
                    {"name": "avatar_0", "x": 35, "y": 60, "z": -1.05},
                    {"name": "h_std_front_blue", "x": 35, "y": 60, "z": -1.05}
                  ],
                  "filters": []
                }
                """.formatted(background));

        assertRgb(0x00FF00, image.getRGB(25, 90));
        assertRgb(0xFF0000, image.getRGB(45, 90));
    }

    @Test
    void untexturedWallTopCapUsesOwnPlaneColor() throws Exception {
        writeSolidSprite("wall_texture_test_white", 8, 8, 0xFFFFFF);
        int background = 0x000000;
        int wallColor = 0xC9A400;

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": %d,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":180,"y":70},{"x":20,"y":150},{"x":180,"y":10},{"x":20,"y":90}],
                      "texCols": [{"assetNames": ["wall_texture_test_white"]}]
                    },
                    {
                      "z": 1,
                      "color": 8021760,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":20,"y":90},{"x":12,"y":86},{"x":180,"y":10},{"x":172,"y":6}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """.formatted(background, wallColor));

        assertRgb(0x7A6700, image.getRGB(100, 48));
    }

    @Test
    void rendersUntexturedRoomPlaneUsingItsPlaneColor() {
        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": %d,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 7320255,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":20,"y":50},{"x":180,"y":130},{"x":20,"y":230},{"x":180,"y":310}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """.formatted(BLUE_BACKGROUND));

        assertRgb(BLUE_TONED_SIDE_WALL, image.getRGB(80, 160));
    }

    @Test
    void mapsFloorTextureColumnsAlongClientWidthAxis() throws Exception {
        writeSolidSprite("floor_texture_test_red", 10, 10, 0xFF0000);
        writeSolidSprite("floor_texture_test_blue", 10, 10, 0x0000FF);

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": 16777215,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16777215,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":100,"y":40},{"x":60,"y":60},{"x":140,"y":60},{"x":100,"y":80}],
                      "texCols": [
                        {"assetNames": ["floor_texture_test_red"]},
                        {"assetNames": ["floor_texture_test_blue"]}
                      ]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """);

        assertRgb(0xFF0000, image.getRGB(115, 53));
        assertRgb(0x0000FF, image.getRGB(105, 58));
    }

    @Test
    void appliesClientTextureOffsetXBeforeTilingPlaneTexture() throws Exception {
        writeHorizontalSplitSprite("floor_texture_offset_x", 4, 4, 0xFF0000, 0x0000FF);

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": 16777215,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16777215,
                      "textureOffsetX": 2,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":100,"y":40},{"x":60,"y":60},{"x":140,"y":60},{"x":100,"y":80}],
                      "texCols": [{"assetNames": ["floor_texture_offset_x"]}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """);

        assertRgb(0x0000FF, image.getRGB(119, 51));
        assertRgb(0xFF0000, image.getRGB(117, 52));
    }

    @Test
    void appliesClientTextureOffsetYBeforeTilingPlaneTexture() throws Exception {
        writeVerticalSplitSprite("floor_texture_offset_y", 4, 4, 0xFF0000, 0x0000FF);

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": 16777215,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16777215,
                      "textureOffsetY": 2,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":100,"y":40},{"x":60,"y":60},{"x":140,"y":60},{"x":100,"y":80}],
                      "texCols": [{"assetNames": ["floor_texture_offset_y"]}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """);

        assertRgb(0x0000FF, image.getRGB(80, 50));
        assertRgb(0xFF0000, image.getRGB(82, 51));
    }

    @Test
    void mapsWallTextureRowsAlongWallHeightAxis() throws Exception {
        writeVerticalSplitSprite("wall_texture_test_horizontal_bands", 4, 4, 0xFF0000, 0x0000FF);

        BufferedImage image = renderScene("""
                {
                  "roomid": 1,
                  "planes": [
                    {
                      "z": 0,
                      "color": 16777215,
                      "cornerPoints": [{"x":0,"y":0},{"x":320,"y":0},{"x":0,"y":320},{"x":320,"y":320}]
                    },
                    {
                      "z": 1,
                      "color": 16777215,
                      "bottomAligned": false,
                      "cornerPoints": [{"x":60,"y":60},{"x":20,"y":60},{"x":60,"y":100},{"x":20,"y":100}],
                      "texCols": [{"assetNames": ["wall_texture_test_horizontal_bands"]}]
                    }
                  ],
                  "sprites": [],
                  "filters": []
                }
                """);

        assertRgb(0x0000FF, image.getRGB(50, 61));
        assertRgb(0x0000FF, image.getRGB(30, 61));
        assertRgb(0xFF0000, image.getRGB(50, 63));
    }

    private BufferedImage renderScene(String json) {
        JSONCamera scene = new Gson().fromJson(json, JSONCamera.class);
        WallColorResolver resolver = new WallColorResolver(null);
        return new CameraRenderImage(scene, 0x000000, spritesDir, null, null, resolver).render();
    }

    private BufferedImage renderScene(String json, String wallPaint, WallColorResolver resolver) {
        JSONCamera scene = new Gson().fromJson(json, JSONCamera.class);
        return new CameraRenderImage(scene, 0x000000, spritesDir, null, wallPaint, resolver).render();
    }

    private void writeSolidSprite(String name, int width, int height, int rgb) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        ImageIO.write(image, "png", spritesDir.resolve(name + ".png").toFile());
    }

    private void writeHorizontalSplitSprite(String name, int width, int height, int leftRgb, int rightRgb) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            int rgb = x < width / 2 ? leftRgb : rightRgb;
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        ImageIO.write(image, "png", spritesDir.resolve(name + ".png").toFile());
    }

    private void writeVerticalSplitSprite(String name, int width, int height, int topRgb, int bottomRgb) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            int rgb = y < height / 2 ? topRgb : bottomRgb;
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        ImageIO.write(image, "png", spritesDir.resolve(name + ".png").toFile());
    }

    private void writeHalfAlphaSprite(String name, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width / 2; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0xFFFFFFFF);
            }
        }
        ImageIO.write(image, "png", spritesDir.resolve(name + ".png").toFile());
    }

    private void assertRgb(int expected, int actual) {
        Color actualColor = new Color(actual);
        Color expectedColor = new Color(expected);
        String message = "expected #%06X but was #%06X".formatted(expected & 0xFFFFFF, actual & 0xFFFFFF);
        assertEquals(expectedColor.getRed(), actualColor.getRed(), 2, "red mismatch: " + message);
        assertEquals(expectedColor.getGreen(), actualColor.getGreen(), 2, "green mismatch: " + message);
        assertEquals(expectedColor.getBlue(), actualColor.getBlue(), 2, "blue mismatch: " + message);
    }
}
