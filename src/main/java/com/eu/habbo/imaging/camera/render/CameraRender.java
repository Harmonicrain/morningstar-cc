package com.eu.habbo.imaging.camera.render;

import com.eu.habbo.Emulator;
import com.eu.habbo.imaging.camera.CameraPaletteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// Client camera JSON order: anchor, height-axis, width-axis, opposite corner.
public abstract class CameraRender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraRender.class);

    private final JSONCamera data;
    private final int width;
    private final int height;
    private final int backgroundColor;

    private BufferedImage render = null;
    private Graphics2D graphics = null;
    private int renderBackgroundColor;

    private final CameraImageLoader imageLoader;
    private final CameraSpriteRenderer spriteRenderer;
    private final CameraPlaneRenderer planeRenderer;
    private final CameraDoorwayRenderer doorwayRenderer;

    public CameraRender(JSONCamera result, int width, int height, int backgroundColor, Path spritesDir, CameraPaletteCache paletteCache, String wallPaint, WallColorResolver wallColorResolver) {
        this.data = result;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.imageLoader = new CameraImageLoader(spritesDir);
        this.spriteRenderer = new CameraSpriteRenderer(this.imageLoader, paletteCache);
        this.planeRenderer = new CameraPlaneRenderer(this.data, this.width, this.height, this.imageLoader, wallPaint, wallColorResolver);
        this.doorwayRenderer = new CameraDoorwayRenderer(this.data, this.width, this.height, this.planeRenderer, this.spriteRenderer);
    }

    public BufferedImage render() {
        if (this.render != null) {
            return this.render;
        }

        this.imageLoader.resetFetchBudget(
                Emulator.getConfig().getInt("camera.image.fetch.max.per.render", 30),
                Emulator.getConfig().getInt("camera.image.fetch.budget.ms", 8000)
        );
        this.renderBackgroundColor = this.planeRenderer.getRenderBackgroundColor(this.backgroundColor);
        this.render = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_RGB);
        this.graphics = this.render.createGraphics();
        this.graphics.setPaint(new Color(this.renderBackgroundColor));
        this.graphics.fillRect(0, 0, this.render.getWidth(), this.render.getHeight());
        // Habbo art is pixel-perfect; antialiasing softens polygon edges and leaves sub-pixel
        // gaps where adjacent planes meet (the 1px background-black line at the wall/floor seam).
        CameraRenderUtils.applyPixelRenderingHints(this.graphics);

        // Render every non-landscape plane, then sprites on top. Sprites south of/in front of
        // walls are fully visible. Masked walls (doorways) won't reveal a character standing
        // in the doorway through their cutout â€” that has to be handled separately on a
        // per-sprite basis, but at least no sprite gets wrongly clipped.
        this.doorwayRenderer.precomputeDoorwayState();
        this.renderByDepth();
        this.renderFilters();
        this.renderEffects();

        this.graphics.dispose();
        return this.render;
    }

    private void renderByDepth() {
        List<RenderLayer> layers = new ArrayList<>();
        int order = 0;
        CameraPlane backgroundPlane = this.planeRenderer.getBackgroundPlane();

        if (this.data.getPlanes() != null) {
            for (CameraPlane plane : this.data.getPlanes()) {
                if (plane == null || plane == backgroundPlane || this.planeRenderer.isLandscapePlane(plane)) {
                    continue;
                }
                layers.add(RenderLayer.forPlane(plane, order++));
            }
        }

        if (this.data.getSprites() != null) {
            for (CameraSprite sprite : this.data.getSprites()) {
                if (sprite == null) {
                    continue;
                }
                layers.add(RenderLayer.forSprite(sprite, sprite.getZ(), order++));
            }
        }

        layers.sort(CameraRender::compareRenderLayers);

        for (RenderLayer layer : layers) {
            if (layer.plane != null) {
                this.planeRenderer.renderSinglePlane(this.graphics, layer.plane);
            } else if (layer.sprite != null) {
                if (!this.doorwayRenderer.isDoorwayAvatarSprite(layer.sprite)) {
                    this.spriteRenderer.renderSingleSprite(this.graphics, layer.sprite);
                }
            }
        }

        this.doorwayRenderer.renderDoorwayOcclusions(this.render, this.graphics, this.renderBackgroundColor);
    }

    private void renderFilters() {
        if (this.data.getFilters() == null) {
            return;
        }

        int w = this.render.getWidth();
        int h = this.render.getHeight();
        int[] pixels = null;

        for (CameraFilter filter : this.data.getFilters()) {
            CameraFilters f = CameraFilters.fromName(filter.getName());

            if (f == CameraFilters.UNKNOWN) {
                LOGGER.debug("Unknown camera filter: {}", filter.getName());
                continue;
            }

            if (f.filterType != CameraFilters.FilterType.COLORMATRIX) {
                continue;
            }

            float[] matrix = f.matrix;
            float filterAlphaTerm = filter.getAlpha() / 255f;
            // render is TYPE_INT_RGB so getRGB returns alpha=255 regardless of pixel.
            int srcA = 255;

            if (pixels == null) {
                pixels = new int[w * h];
            }
            this.render.getRGB(0, 0, w, h, pixels, 0, w);

            for (int i = 0; i < pixels.length; i++) {
                int pixel = pixels[i];
                int srcR = (pixel >> 16) & 0xFF;
                int srcG = (pixel >> 8) & 0xFF;
                int srcB = pixel & 0xFF;

                float red = matrix[0] * srcR + matrix[1] * srcG + matrix[2] * srcB + matrix[3] * srcA + matrix[4] * filterAlphaTerm;
                float green = matrix[5] * srcR + matrix[6] * srcG + matrix[7] * srcB + matrix[8] * srcA + matrix[9] * filterAlphaTerm;
                float blue = matrix[10] * srcR + matrix[11] * srcG + matrix[12] * srcB + matrix[13] * srcA + matrix[14] * filterAlphaTerm;
                float a = matrix[15] * srcR + matrix[16] * srcG + matrix[17] * srcB + matrix[18] * srcA + matrix[19] * filterAlphaTerm;

                int rOut = CameraRenderUtils.clampByte(red);
                int gOut = CameraRenderUtils.clampByte(green);
                int bOut = CameraRenderUtils.clampByte(blue);
                int aOut = CameraRenderUtils.clampByte(a);

                pixels[i] = (aOut << 24) | (rOut << 16) | (gOut << 8) | bOut;
            }

            this.render.setRGB(0, 0, w, h, pixels, 0, w);
        }
    }

    private void renderEffects() {
        if (this.data.getFilters() == null) {
            return;
        }

        for (CameraFilter modifier : this.data.getFilters()) {
            CameraFilters filter = CameraFilters.fromName(modifier.getName());

            if (filter == null || filter.filterType != CameraFilters.FilterType.COMPOSITE) {
                continue;
            }

            File effectFile = this.imageLoader.spriteFile(modifier.getName());
            if (effectFile == null) {
                continue;
            }

            BufferedImage effect;
            try {
                effect = ImageIO.read(effectFile);
            } catch (Exception e) {
                continue;
            }

            if (effect == null) {
                continue;
            }

            if (filter.blendMode == CameraFilters.BlendMode.HARDLIGHT) {
                for (int x = 0; x < effect.getWidth(); ++x) {
                    for (int y = 0; y < effect.getHeight(); ++y) {
                        Color color = new Color(effect.getRGB(x, y));

                        float a = color.getAlpha() * (modifier.getAlpha() / 255f);

                        effect.setRGB(x, y, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) a).getRGB());
                    }
                }

                this.graphics.drawImage(effect, 0, 0, null);
            } else if (filter.blendMode == CameraFilters.BlendMode.NORMAL) {
                this.graphics.drawImage(effect, 0, 0, null);
            } else {
                for (int x = 0; x < effect.getWidth(); ++x) {
                    for (int y = 0; y < effect.getHeight(); ++y) {
                        Color color = new Color(effect.getRGB(x, y));

                        float a = color.getAlpha() * (modifier.getAlpha() / 255f);

                        if (a > 255) {
                            a = 255;
                        }

                        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) a);

                        if (filter.blendMode == CameraFilters.BlendMode.MULTIPLY) {
                            Color renderColor = new Color(this.render.getRGB(x, y));
                            float red = (renderColor.getRed() / 255.0F) * (color.getRed() / 255.0F);
                            float green = (renderColor.getGreen() / 255.0F) * (color.getGreen() / 255.0F);
                            float blue = (renderColor.getBlue() / 255.0F) * (color.getBlue() / 255.0F);
                            float newAlpha = (renderColor.getAlpha() / 255.0F) * (color.getAlpha() / 255.0F);

                            this.render.setRGB(x, y, new Color(red, green, blue, newAlpha).getRGB());
                        } else if (filter.blendMode == CameraFilters.BlendMode.OVERLAY) {
                            Color renderColor = new Color(this.render.getRGB(x, y));
                            float red = (renderColor.getRed()) + (color.getRed());
                            float green = (renderColor.getGreen()) + (color.getGreen());
                            float blue = (renderColor.getBlue()) + (color.getBlue());
                            float newAlpha = (renderColor.getAlpha()) + (color.getAlpha());

                            if (red > 255) red = 255;
                            if (red < 0) red = 0;
                            if (green > 255) green = 255;
                            if (green < 0) green = 0;
                            if (blue > 255) blue = 255;
                            if (blue < 0) blue = 0;
                            if (newAlpha > 255) newAlpha = 255;
                            if (newAlpha < 0) newAlpha = 0;

                            this.render.setRGB(x, y, new Color(red / 255.0F, green / 255.0F, blue / 255.0F, newAlpha / 255.0F).getRGB());
                        }
                    }
                }
            }
        }
    }

    private static int compareRenderLayers(RenderLayer left, RenderLayer right) {
        int depthCompare = Double.compare(right.depth, left.depth);
        if (depthCompare != 0) {
            return depthCompare;
        }
        return Integer.compare(left.order, right.order);
    }

    private record RenderLayer(double depth, int order, CameraPlane plane, CameraSprite sprite) {
        private static RenderLayer forPlane(CameraPlane plane, int order) {
            return new RenderLayer(plane.getZ(), order, plane, null);
        }

        private static RenderLayer forSprite(CameraSprite sprite, double depth, int order) {
            return new RenderLayer(depth, order, null, sprite);
        }
    }
}
