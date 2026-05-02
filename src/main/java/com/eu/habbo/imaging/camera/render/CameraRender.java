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

    public CameraRender(JSONCamera result, int width, int height, int backgroundColor, Path spritesDir, Path framesDir, CameraPaletteCache paletteCache, String wallPaint, WallColorResolver wallColorResolver) {
        this.data = result;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.imageLoader = new CameraImageLoader(spritesDir, framesDir);
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
        try {
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
        } finally {
            this.graphics.dispose();
        }
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
            float strength = filter.getAlpha() / 255f;
            float weakness = 1f - strength;
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

                // Apply the full colormatrix unconditionally, then lerp toward the original
                // pixel by (1 - strength). This matches the Flash client behaviour where the
                // percentage slider blends between the unfiltered and fully-filtered image.
                float red   = matrix[0]  * srcR + matrix[1]  * srcG + matrix[2]  * srcB + matrix[3]  * srcA + matrix[4];
                float green = matrix[5]  * srcR + matrix[6]  * srcG + matrix[7]  * srcB + matrix[8]  * srcA + matrix[9];
                float blue  = matrix[10] * srcR + matrix[11] * srcG + matrix[12] * srcB + matrix[13] * srcA + matrix[14];

                int rOut = CameraRenderUtils.clampByte(srcR * weakness + red   * strength);
                int gOut = CameraRenderUtils.clampByte(srcG * weakness + green * strength);
                int bOut = CameraRenderUtils.clampByte(srcB * weakness + blue  * strength);

                pixels[i] = (0xFF << 24) | (rOut << 16) | (gOut << 8) | bOut;
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

            if (filter == null || (filter.filterType != CameraFilters.FilterType.COMPOSITE && filter.filterType != CameraFilters.FilterType.FRAME)) {
                continue;
            }

            boolean isFrame = filter.filterType == CameraFilters.FilterType.FRAME;
            // Camera effect overlays are stored under frames.path in this deployment.
            // Load both FRAME and COMPOSITE filters from the frames directory.
            File effectFile = this.imageLoader.frameFile(modifier.getName());
            if (effectFile == null) {
                continue;
            }

            BufferedImage effect;
            try {
                effect = ImageIO.read(effectFile);
            } catch (Exception e) {
                LOGGER.debug("Failed to load {} '{}': {}", isFrame ? "frame" : "effect sprite", modifier.getName(), e.getMessage());
                continue;
            }

            if (effect == null) {
                continue;
            }

            if (isFrame) {
                this.graphics.drawImage(effect, 0, 0, null);
            } else if (filter.blendMode == CameraFilters.BlendMode.HARDLIGHT) {
                for (int x = 0; x < effect.getWidth(); ++x) {
                    for (int y = 0; y < effect.getHeight(); ++y) {
                        int rgb = effect.getRGB(x, y);
                        int srcA = (rgb >> 24) & 0xFF;
                        int scaledA = Math.min(255, (int) (srcA * (modifier.getAlpha() / 255f)));
                        effect.setRGB(x, y, (scaledA << 24) | (rgb & 0x00FFFFFF));
                    }
                }

                this.graphics.drawImage(effect, 0, 0, null);
            } else if (filter.blendMode == CameraFilters.BlendMode.NORMAL) {
                this.graphics.drawImage(effect, 0, 0, null);
            } else {
                int maxX = Math.min(effect.getWidth(), this.render.getWidth());
                int maxY = Math.min(effect.getHeight(), this.render.getHeight());
                for (int x = 0; x < maxX; ++x) {
                    for (int y = 0; y < maxY; ++y) {
                        int eRgb = effect.getRGB(x, y);
                        int eA = (eRgb >> 24) & 0xFF;
                        int scaledA = Math.min(255, (int) (eA * (modifier.getAlpha() / 255f)));
                        int eR = (eRgb >> 16) & 0xFF;
                        int eG = (eRgb >> 8) & 0xFF;
                        int eB = eRgb & 0xFF;

                        int rRgb = this.render.getRGB(x, y);
                        int rA = (rRgb >> 24) & 0xFF;
                        int rR = (rRgb >> 16) & 0xFF;
                        int rG = (rRgb >> 8) & 0xFF;
                        int rB = rRgb & 0xFF;

                        int outR, outG, outB, outA;
                        float alphaFactor = scaledA / 255f;
                        if (filter.blendMode == CameraFilters.BlendMode.MULTIPLY) {
                            int blendR = (rR * eR) / 255;
                            int blendG = (rG * eG) / 255;
                            int blendB = (rB * eB) / 255;
                            outR = CameraRenderUtils.clampByte(Math.round((rR * (1f - alphaFactor)) + (blendR * alphaFactor)));
                            outG = CameraRenderUtils.clampByte(Math.round((rG * (1f - alphaFactor)) + (blendG * alphaFactor)));
                            outB = CameraRenderUtils.clampByte(Math.round((rB * (1f - alphaFactor)) + (blendB * alphaFactor)));
                            outA = rA;
                        } else { // OVERLAY
                            int blendR = Math.min(255, Math.max(0, rR + eR));
                            int blendG = Math.min(255, Math.max(0, rG + eG));
                            int blendB = Math.min(255, Math.max(0, rB + eB));
                            outR = CameraRenderUtils.clampByte(Math.round((rR * (1f - alphaFactor)) + (blendR * alphaFactor)));
                            outG = CameraRenderUtils.clampByte(Math.round((rG * (1f - alphaFactor)) + (blendG * alphaFactor)));
                            outB = CameraRenderUtils.clampByte(Math.round((rB * (1f - alphaFactor)) + (blendB * alphaFactor)));
                            outA = rA;
                        }
                        this.render.setRGB(x, y, (outA << 24) | (outR << 16) | (outG << 8) | outB);
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
