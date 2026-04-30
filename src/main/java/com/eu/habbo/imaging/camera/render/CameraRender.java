package com.eu.habbo.imaging.camera.render;

import com.eu.habbo.imaging.camera.CameraPalette;
import com.eu.habbo.imaging.camera.CameraPaletteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Client camera JSON order: anchor, height-axis, width-axis, opposite corner.
public abstract class CameraRender {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraRender.class);
    private static final double DOORWAY_CLUSTER_DEPTH_TOLERANCE = 0.5;
    private static final double PENDING_AVATAR_DEPTH_WEIGHT = 64.0;

    private final JSONCamera data;
    private final int width;
    private final int height;
    private final int backgroundColor;
    private final Path spritesDir;
    private final CameraPaletteCache paletteCache;
    private final String wallPaint;
    private final WallColorResolver wallColorResolver;

    private BufferedImage render = null;
    private Graphics2D graphics = null;
    private int renderBackgroundColor;

    private final BasicStroke simpleStroke = new BasicStroke(0);
    private final Map<String, BufferedImage> spriteCache = new HashMap<>();
    private final Map<CameraPlane, DoorwayMask> doorwayMaskCache = new IdentityHashMap<>();
    private final Map<CameraPlane, DoorwayMask> avatarRevealMaskCache = new IdentityHashMap<>();
    private final Set<CameraSprite> doorwayAvatarSprites = Collections.newSetFromMap(new IdentityHashMap<>());

    public CameraRender(JSONCamera result, int width, int height, int backgroundColor, Path spritesDir, CameraPaletteCache paletteCache, String wallPaint, WallColorResolver wallColorResolver) {
        this.data = result;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.spritesDir = spritesDir;
        this.paletteCache = paletteCache;
        this.wallPaint = wallPaint;
        this.wallColorResolver = wallColorResolver;
    }

    public BufferedImage render() {
        if (this.render != null)
            return this.render;

        this.renderBackgroundColor = getRenderBackgroundColor();
        this.render = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        this.graphics = this.render.createGraphics();
        this.graphics.setPaint(new Color(this.renderBackgroundColor));
        this.graphics.fillRect(0, 0, this.render.getWidth(), this.render.getHeight());
        // Habbo art is pixel-perfect; antialiasing softens polygon edges and leaves sub-pixel
        // gaps where adjacent planes meet (the 1px background-black line at the wall/floor seam).
        this.graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        this.graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        this.graphics.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        this.graphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        // STROKE_PURE keeps polygon edges on integer pixel boundaries; the default NORMALIZE
        // shifts edges by 0.5px to "look better" with antialiasing, but with AA off that just
        // produces sub-pixel rasterisation gaps where adjacent polygons meet.
        this.graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        // Render every non-landscape plane, then sprites on top. Sprites south of/in front of
        // walls are fully visible. Masked walls (doorways) won't reveal a character standing
        // in the doorway through their cutout — that has to be handled separately on a
        // per-sprite basis, but at least no sprite gets wrongly clipped.
        this.precomputeDoorwayState();
        this.renderByDepth();
        this.renderFilters();
        this.renderEffects();

        this.graphics.dispose();
        return this.render;
    }

    private File spriteFile(String asset) {
        if (asset == null || asset.isEmpty()) {
            return null;
        }
        // Normalize and confirm the resolved path stays inside spritesDir, so a sprite
        // name from JSON like "../../config/server" can't escape the assets directory.
        Path resolved = this.spritesDir.resolve(asset + ".png").normalize();
        if (!resolved.startsWith(this.spritesDir)) {
            return null;
        }
        return resolved.toFile();
    }

    private void renderByDepth() {
        List<RenderLayer> layers = new ArrayList<>();
        int order = 0;
        CameraPlane backgroundPlane = getBackgroundPlane();

        if (data.getPlanes() != null) {
            for (CameraPlane plane : data.getPlanes()) {
                if (plane == null || plane == backgroundPlane || isLandscapePlane(plane)) {
                    continue;
                }
                layers.add(RenderLayer.forPlane(plane, order++));
            }
        }

        if (data.getSprites() != null) {
            for (CameraSprite sprite : data.getSprites()) {
                if (sprite == null) {
                    continue;
                }
                layers.add(RenderLayer.forSprite(sprite, sprite.getZ(), order++));
            }
        }

        layers.sort(CameraRender::compareRenderLayers);

        for (RenderLayer layer : layers) {
            if (layer.plane != null) {
                renderSinglePlane(layer.plane);
            } else if (layer.sprite != null) {
                if (!isDoorwayAvatarSprite(layer.sprite)) {
                    renderSingleSprite(layer.sprite);
                }
            }
        }

        renderDoorwayOcclusions();
    }

    private void precomputeDoorwayState() {
        this.doorwayAvatarSprites.clear();
        this.doorwayMaskCache.clear();
        this.avatarRevealMaskCache.clear();

        if (data.getPlanes() == null || data.getSprites() == null) {
            return;
        }

        List<AvatarSpriteCluster> avatarClusters = buildAvatarSpriteClusters();
        for (CameraPlane plane : data.getPlanes()) {
            if (plane == null || !isWallPlane(plane) || !hasPlaneMasks(plane)) {
                continue;
            }

            DoorwayMask doorwayMask = getDoorwayMask(plane);
            if (doorwayMask == null || doorwayMask.bounds.isEmpty()) {
                continue;
            }

            Rectangle candidateBounds = expandRectangle(doorwayMask.bounds, 32);
            List<AvatarSpriteCluster> overlappingClusters = new ArrayList<>();
            double backmostClusterDepth = Double.NEGATIVE_INFINITY;
            for (AvatarSpriteCluster cluster : avatarClusters) {
                if (!cluster.intersects(candidateBounds)) {
                    continue;
                }

                if (!clusterOverlapsMask(cluster, doorwayMask)) {
                    continue;
                }
                overlappingClusters.add(cluster);
                backmostClusterDepth = Math.max(backmostClusterDepth, cluster.referenceDepth());
            }

            for (AvatarSpriteCluster cluster : overlappingClusters) {
                if ((backmostClusterDepth - cluster.referenceDepth()) > DOORWAY_CLUSTER_DEPTH_TOLERANCE) {
                    continue;
                }
                for (CameraSprite sprite : cluster.sprites()) {
                    this.doorwayAvatarSprites.add(sprite);
                }
            }
        }
    }

    private boolean spriteIntersects(CameraSprite sprite, Rectangle bounds) {
        Rectangle spriteBounds = getSpriteBounds(sprite);
        if (spriteBounds == null) {
            return false;
        }

        return spriteBounds.intersects(bounds);
    }

    private List<AvatarSpriteCluster> buildAvatarSpriteClusters() {
        List<AvatarSpriteCluster> clusters = new ArrayList<>();
        List<CameraSprite> pendingSprites = new ArrayList<>();

        for (CameraSprite sprite : data.getSprites()) {
            if (sprite == null || !isLikelyAvatarSprite(sprite)) {
                continue;
            }

            Rectangle spriteBounds = getSpriteBounds(sprite);
            if (isAvatarAnchorSprite(sprite)) {
                AvatarSpriteCluster cluster = new AvatarSpriteCluster(sprite, spriteBounds);
                cluster.add(sprite, spriteBounds);
                clusters.add(cluster);
                continue;
            }

            pendingSprites.add(sprite);
        }

        if (clusters.isEmpty()) {
            if (!pendingSprites.isEmpty()) {
                AvatarSpriteCluster fallbackCluster = new AvatarSpriteCluster(null, null);
                for (CameraSprite sprite : pendingSprites) {
                    fallbackCluster.add(sprite, getSpriteBounds(sprite));
                }
                clusters.add(fallbackCluster);
            }
            return clusters;
        }

        for (CameraSprite pendingSprite : pendingSprites) {
            Rectangle pendingBounds = getSpriteBounds(pendingSprite);
            AvatarSpriteCluster bestCluster = findBestPendingAvatarCluster(clusters, pendingSprite, pendingBounds);
            bestCluster.add(pendingSprite, pendingBounds);
        }

        return clusters;
    }

    private AvatarSpriteCluster findBestPendingAvatarCluster(List<AvatarSpriteCluster> clusters, CameraSprite sprite, Rectangle spriteBounds) {
        AvatarSpriteCluster bestCluster = clusters.get(0);
        double bestScore = Double.POSITIVE_INFINITY;

        for (AvatarSpriteCluster cluster : clusters) {
            double score = pendingAvatarClusterScore(cluster, sprite, spriteBounds);
            if (score < bestScore) {
                bestScore = score;
                bestCluster = cluster;
            }
        }

        return bestCluster;
    }

    private double pendingAvatarClusterScore(AvatarSpriteCluster cluster, CameraSprite sprite, Rectangle spriteBounds) {
        Rectangle clusterBounds = cluster.assignmentBounds();
        double spatialScore = rectangleCenterDistanceSquared(spriteBounds, clusterBounds);
        double depthPenalty = isAvatarShadowSprite(sprite)
                ? 0.0
                : Math.abs(sprite.getZ() - cluster.referenceDepth()) * PENDING_AVATAR_DEPTH_WEIGHT;
        return spatialScore + depthPenalty;
    }

    private double rectangleCenterDistanceSquared(Rectangle first, Rectangle second) {
        if (first == null || second == null) {
            return 0.0;
        }

        double centerXFirst = first.getCenterX();
        double centerYFirst = first.getCenterY();
        double centerXSecond = second.getCenterX();
        double centerYSecond = second.getCenterY();
        double dx = centerXFirst - centerXSecond;
        double dy = centerYFirst - centerYSecond;
        return (dx * dx) + (dy * dy);
    }

    private boolean clusterOverlapsMask(AvatarSpriteCluster cluster, DoorwayMask doorwayMask) {
        int shadowOverlap = clusterMaskOverlap(cluster, doorwayMask, this::isAvatarShadowSprite, null);
        if (shadowOverlap > 0) {
            return true;
        }

        if (cluster.hasSpriteMatching(this::isAvatarShadowSprite)) {
            return false;
        }

        int footprintOverlap = clusterMaskOverlap(cluster, doorwayMask, this::isAvatarFootprintSprite, null);
        if (footprintOverlap > 0) {
            return true;
        }

        if (cluster.hasSpriteMatching(this::isAvatarFootprintSprite)) {
            return false;
        }

        Rectangle footprintBounds = cluster.doorwayFootprintBounds();
        if (clusterMaskOverlap(cluster, doorwayMask, sprite -> true, footprintBounds) > 0) {
            return true;
        }

        return false;
    }

    private int clusterMaskOverlap(AvatarSpriteCluster cluster, DoorwayMask doorwayMask, java.util.function.Predicate<CameraSprite> predicate, Rectangle clipBounds) {
        int overlap = 0;
        for (CameraSprite sprite : cluster.sprites()) {
            if (!predicate.test(sprite)) {
                continue;
            }
            overlap += spriteMaskOverlapPixelCount(sprite, doorwayMask, clipBounds);
        }
        return overlap;
    }

    private boolean spriteOverlapsMask(CameraSprite sprite, DoorwayMask doorwayMask) {
        return spriteOverlapsMask(sprite, doorwayMask, null);
    }

    private boolean spriteOverlapsMask(CameraSprite sprite, DoorwayMask doorwayMask, Rectangle clipBounds) {
        return spriteMaskOverlapPixelCount(sprite, doorwayMask, clipBounds) > 0;
    }

    private int spriteMaskOverlapPixelCount(CameraSprite sprite, DoorwayMask doorwayMask, Rectangle clipBounds) {
        Rectangle spriteBounds = getSpriteBounds(sprite);
        if (spriteBounds == null) {
            return 0;
        }

        Rectangle intersection = spriteBounds.intersection(doorwayMask.bounds);
        if (clipBounds != null) {
            intersection = intersection.intersection(clipBounds);
        }
        if (intersection.isEmpty()) {
            return 0;
        }

        BufferedImage spriteImage = getSpriteImage(sprite);
        if (spriteImage == null) {
            return 0;
        }

        int overlapPixels = 0;
        for (int x = intersection.x; x < intersection.x + intersection.width; x++) {
            int imageX = x - spriteBounds.x;
            if (sprite.isFlipH()) {
                imageX = spriteImage.getWidth() - 1 - imageX;
            }
            if (imageX < 0 || imageX >= spriteImage.getWidth()) {
                continue;
            }

            for (int y = intersection.y; y < intersection.y + intersection.height; y++) {
                if (((doorwayMask.alpha.getRGB(x, y) >>> 24) & 0xFF) == 0) {
                    continue;
                }

                int imageY = y - spriteBounds.y;
                if (imageY < 0 || imageY >= spriteImage.getHeight()) {
                    continue;
                }

                if (((spriteImage.getRGB(imageX, imageY) >>> 24) & 0xFF) > 0) {
                    overlapPixels++;
                }
            }
        }

        return overlapPixels;
    }

    private Rectangle getSpriteBounds(CameraSprite sprite) {
        if (sprite == null) {
            return null;
        }

        BufferedImage spriteImage = getSpriteImage(sprite);
        if (spriteImage == null) {
            return new Rectangle(sprite.getX(), sprite.getY(), 1, 1);
        }

        return new Rectangle(sprite.getX(), sprite.getY(), spriteImage.getWidth(), spriteImage.getHeight());
    }

    private BufferedImage getSpriteImage(CameraSprite sprite) {
        if (sprite == null) {
            return null;
        }

        return sprite.isFromUrl() ? readUrlImage(sprite.getName()) : readSprite(sprite.getName());
    }

    private boolean isAvatarFootprintSprite(CameraSprite sprite) {
        String part = getAvatarSpritePart(sprite);
        return "sd".equals(part) || "lg".equals(part) || "sh".equals(part) || "wa".equals(part);
    }

    private boolean isAvatarShadowSprite(CameraSprite sprite) {
        return "sd".equals(getAvatarSpritePart(sprite));
    }

    private boolean isAvatarAnchorSprite(CameraSprite sprite) {
        String name = sprite.getName();
        return name != null && name.startsWith("avatar_");
    }

    private String getAvatarSpritePart(CameraSprite sprite) {
        if (sprite == null) {
            return null;
        }

        String name = sprite.getName();
        if (name == null) {
            return null;
        }

        String[] parts = name.split("_");
        return parts.length > 2 ? parts[2] : null;
    }

    private boolean isLikelyAvatarSprite(CameraSprite sprite) {
        String name = sprite.getName();
        if (name == null || name.isEmpty() || sprite.isFromUrl()) {
            return false;
        }

        return name.startsWith("avatar_")
                || name.startsWith("h_")
                || name.startsWith("hd_")
                || name.startsWith("hr_")
                || name.startsWith("ha_")
                || name.startsWith("he_")
                || name.startsWith("ea_")
                || name.startsWith("fa_")
                || name.startsWith("ch_")
                || name.startsWith("cc_")
                || name.startsWith("cp_")
                || name.startsWith("ca_")
                || name.startsWith("lg_")
                || name.startsWith("sh_")
                || name.startsWith("wa_");
    }

    private void renderDoorwayOcclusions() {
        if (data.getPlanes() == null) {
            return;
        }

        for (int planeIndex = 0; planeIndex < data.getPlanes().length; planeIndex++) {
            CameraPlane plane = data.getPlanes()[planeIndex];
            if (plane == null || !isWallPlane(plane) || !hasPlaneMasks(plane)) {
                continue;
            }

            DoorwayMask doorwayMask = getDoorwayMask(plane);
            if (doorwayMask == null || doorwayMask.bounds.isEmpty()) {
                continue;
            }

            // Repaint the masked wall face after the main z-pass so any same-depth floor
            // planes or wide avatar parts that spilled over the wall get covered again.
            renderSinglePlane(plane);
            fillDoorwayMask(doorwayMask);
            renderForegroundPlanesOverDoorway(planeIndex, doorwayMask, true);
            renderDoorwayAvatarReveals(planeIndex, doorwayMask);
            renderForegroundPlanesOverDoorway(planeIndex, doorwayMask, false);
            renderForegroundSpritesOverWall(plane, doorwayMask);
        }
    }

    private void renderDoorwayAvatarReveals(int maskedWallPlaneIndex, DoorwayMask doorwayMask) {
        if (data.getSprites() == null) {
            return;
        }

        DoorwayMask revealMask = getAvatarDoorwayRevealMask(maskedWallPlaneIndex, doorwayMask);
        for (CameraSprite sprite : data.getSprites()) {
            if (sprite == null || !this.doorwayAvatarSprites.contains(sprite)) {
                continue;
            }
            renderSingleSpriteClippedToMask(sprite, revealMask);
        }
    }

    private void renderForegroundPlanesOverDoorway(int maskedWallPlaneIndex, DoorwayMask doorwayMask, boolean floorPlanes) {
        if (data.getPlanes() == null) {
            return;
        }

        if (floorPlanes) {
            renderFloorPlanesOverDoorway(doorwayMask);
            return;
        }

        CameraPlane backgroundPlane = getBackgroundPlane();
        for (int planeIndex = maskedWallPlaneIndex + 1; planeIndex < data.getPlanes().length; planeIndex++) {
            CameraPlane plane = data.getPlanes()[planeIndex];
            if (plane == null || plane == backgroundPlane || isLandscapePlane(plane)) {
                continue;
            }

            if (isWallPlane(plane) && hasPlaneMasks(plane)) {
                continue;
            }

            if (isFloorPlane(plane) != floorPlanes) {
                continue;
            }

            if (planeIntersects(plane, doorwayMask.bounds)) {
                renderSinglePlaneClippedToMask(plane, doorwayMask);
            }
        }
    }

    private void renderFloorPlanesOverDoorway(DoorwayMask doorwayMask) {
        List<RenderLayer> floorLayers = new ArrayList<>();
        CameraPlane backgroundPlane = getBackgroundPlane();
        int order = 0;

        for (CameraPlane plane : data.getPlanes()) {
            if (plane == null || plane == backgroundPlane || isLandscapePlane(plane)) {
                continue;
            }

            if (isFloorPlane(plane) && planeIntersects(plane, doorwayMask.bounds)) {
                floorLayers.add(RenderLayer.forPlane(plane, order));
            }

            order++;
        }

        floorLayers.sort(CameraRender::compareRenderLayers);
        for (RenderLayer layer : floorLayers) {
            renderSinglePlaneClippedToMask(layer.plane, doorwayMask);
        }
    }

    private void renderForegroundSpritesOverWall(CameraPlane plane, DoorwayMask doorwayMask) {
        if (data.getSprites() == null) {
            return;
        }

        Rectangle wallBounds = getPlanePolygon(plane.getCornerPoints()).getBounds();
        for (CameraSprite sprite : data.getSprites()) {
            if (sprite == null || isDoorwayAvatarSprite(sprite) || sprite.getZ() >= plane.getZ()) {
                continue;
            }
            if (spriteIntersects(sprite, wallBounds)) {
                renderSingleSprite(sprite);
            }
        }
    }

    private void fillDoorwayMask(DoorwayMask doorwayMask) {
        Rectangle bounds = doorwayMask.bounds;
        int x0 = Math.max(0, bounds.x);
        int y0 = Math.max(0, bounds.y);
        int x1 = Math.min(this.width, bounds.x + bounds.width);
        int y1 = Math.min(this.height, bounds.y + bounds.height);
        int rw = x1 - x0;
        int rh = y1 - y0;
        if (rw <= 0 || rh <= 0) {
            return;
        }

        int[] maskPixels = new int[rw * rh];
        doorwayMask.alpha.getRGB(x0, y0, rw, rh, maskPixels, 0, rw);
        int[] renderPixels = new int[rw * rh];
        this.render.getRGB(x0, y0, rw, rh, renderPixels, 0, rw);

        int fill = 0xFF000000 | (this.renderBackgroundColor & 0x00FFFFFF);
        boolean changed = false;
        for (int i = 0; i < maskPixels.length; i++) {
            if (((maskPixels[i] >>> 24) & 0xFF) > 0) {
                renderPixels[i] = fill;
                changed = true;
            }
        }
        if (changed) {
            this.render.setRGB(x0, y0, rw, rh, renderPixels, 0, rw);
        }
    }

    private boolean isDoorwayAvatarSprite(CameraSprite sprite) {
        return sprite != null && this.doorwayAvatarSprites.contains(sprite);
    }

    private DoorwayMask getDoorwayMask(CameraPlane plane) {
        if (this.doorwayMaskCache.containsKey(plane)) {
            return this.doorwayMaskCache.get(plane);
        }

        DoorwayMask doorwayMask = buildDoorwayMask(plane);
        this.doorwayMaskCache.put(plane, doorwayMask);
        return doorwayMask;
    }

    private DoorwayMask buildDoorwayMask(CameraPlane plane) {
        CameraPositionPoint[] points = plane.getCornerPoints();
        if (points == null || points.length < 4) {
            return null;
        }

        PlaneStyle planeStyle = getPlaneStyle(plane, new Color(plane.getColor()));
        BufferedImage texture = buildPlaneTexture(plane, planeStyle);
        if (texture == null) {
            return null;
        }

        if (isWallPlane(plane)) {
            texture = flipVertical(texture);
            texture = CameraUtils.flipHorizontal(texture);
        }

        AffineTransform transform = getTextureTransform(plane, texture.getWidth(), texture.getHeight());
        if (transform == null) {
            return null;
        }

        Polygon polygon = getPlanePolygon(points);
        BufferedImage renderedWall = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D wallGraphics = renderedWall.createGraphics();
        applyPixelRenderingHints(wallGraphics);
        wallGraphics.setClip(polygon);
        wallGraphics.drawImage(texture, transform, null);
        wallGraphics.dispose();

        Rectangle polygonBounds = polygon.getBounds();
        BufferedImage renderMask = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        for (int x = polygonBounds.x; x < polygonBounds.x + polygonBounds.width; x++) {
            if (x < 0 || x >= this.width) {
                continue;
            }
            for (int y = polygonBounds.y; y < polygonBounds.y + polygonBounds.height; y++) {
                if (y < 0 || y >= this.height || !polygon.contains(x + 0.5, y + 0.5)) {
                    continue;
                }
                if (((renderedWall.getRGB(x, y) >>> 24) & 0xFF) == 0) {
                    renderMask.setRGB(x, y, 0xFFFFFFFF);
                }
            }
        }

        Rectangle bounds = getAlphaBounds(renderMask);
        return bounds.isEmpty() ? null : new DoorwayMask(renderMask, bounds);
    }

    private boolean planeIntersects(CameraPlane plane, Rectangle bounds) {
        CameraPositionPoint[] points = plane.getCornerPoints();
        if (points == null || points.length < 4) {
            return false;
        }

        return getPlanePolygon(points).getBounds().intersects(bounds);
    }

    private DoorwayMask getAvatarDoorwayRevealMask(int maskedWallPlaneIndex, DoorwayMask doorwayMask) {
        CameraPlane plane = data.getPlanes()[maskedWallPlaneIndex];
        if (this.avatarRevealMaskCache.containsKey(plane)) {
            return this.avatarRevealMaskCache.get(plane);
        }

        DoorwayMask revealMask = buildAvatarDoorwayRevealMask(maskedWallPlaneIndex, doorwayMask);
        this.avatarRevealMaskCache.put(plane, revealMask);
        return revealMask;
    }

    private DoorwayMask buildAvatarDoorwayRevealMask(int maskedWallPlaneIndex, DoorwayMask doorwayMask) {
        Rectangle bounds = doorwayMask.bounds;
        BufferedImage foregroundPlaneMask = buildForegroundPlaneMask(maskedWallPlaneIndex, bounds, false);
        foregroundPlaneMask = expandMask(foregroundPlaneMask, bounds, 10);
        BufferedImage revealMask = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);

        for (int x = bounds.x; x < bounds.x + bounds.width; x++) {
            if (x < 0 || x >= this.width) {
                continue;
            }
            for (int y = bounds.y; y < bounds.y + bounds.height; y++) {
                if (y < 0 || y >= this.height) {
                    continue;
                }
                if (((foregroundPlaneMask.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    continue;
                }
                revealMask.setRGB(x, y, doorwayMask.alpha.getRGB(x, y));
            }
        }

        Rectangle revealBounds = getAlphaBounds(revealMask);
        return revealBounds.isEmpty() ? doorwayMask : new DoorwayMask(revealMask, revealBounds);
    }

    private BufferedImage buildForegroundPlaneMask(int maskedWallPlaneIndex, Rectangle bounds, boolean includeFloorPlanes) {
        BufferedImage planeMask = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D maskGraphics = planeMask.createGraphics();
        applyPixelRenderingHints(maskGraphics);
        maskGraphics.setColor(Color.WHITE);

        CameraPlane backgroundPlane = getBackgroundPlane();
        for (int planeIndex = maskedWallPlaneIndex + 1; planeIndex < data.getPlanes().length; planeIndex++) {
            CameraPlane plane = data.getPlanes()[planeIndex];
            if (plane == null || plane == backgroundPlane || isLandscapePlane(plane)) {
                continue;
            }

            if (isFloorPlane(plane) != includeFloorPlanes) {
                continue;
            }

            if (!planeIntersects(plane, bounds)) {
                continue;
            }

            CameraPositionPoint[] points = plane.getCornerPoints();
            if (points == null || points.length < 4) {
                continue;
            }

            maskGraphics.fillPolygon(getPlanePolygon(points));
        }

        maskGraphics.dispose();
        return planeMask;
    }

    private BufferedImage expandMask(BufferedImage sourceMask, Rectangle bounds, int radius) {
        if (radius <= 0) {
            return sourceMask;
        }

        // Square (Chebyshev) dilation factors into a horizontal pass followed by a vertical
        // pass — same result, O(W*H*r) instead of O(W*H*r*r).
        int w = this.width;
        int h = this.height;
        int[] sourcePixels = new int[w * h];
        sourceMask.getRGB(0, 0, w, h, sourcePixels, 0, w);

        int x0 = Math.max(0, bounds.x);
        int y0 = Math.max(0, bounds.y);
        int x1 = Math.min(w, bounds.x + bounds.width);
        int y1 = Math.min(h, bounds.y + bounds.height);

        int hStart = Math.max(0, y0 - radius);
        int hEnd = Math.min(h, y1 + radius);

        boolean[] horizontal = new boolean[w * h];
        for (int y = hStart; y < hEnd; y++) {
            int rowOffset = y * w;
            for (int x = x0; x < x1; x++) {
                int xMin = Math.max(0, x - radius);
                int xMax = Math.min(w - 1, x + radius);
                for (int sx = xMin; sx <= xMax; sx++) {
                    if (((sourcePixels[rowOffset + sx] >>> 24) & 0xFF) > 0) {
                        horizontal[rowOffset + x] = true;
                        break;
                    }
                }
            }
        }

        int[] outPixels = new int[w * h];
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                int yMin = Math.max(0, y - radius);
                int yMax = Math.min(h - 1, y + radius);
                for (int sy = yMin; sy <= yMax; sy++) {
                    if (horizontal[sy * w + x]) {
                        outPixels[y * w + x] = 0xFFFFFFFF;
                        break;
                    }
                }
            }
        }

        BufferedImage expandedMask = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        expandedMask.setRGB(0, 0, w, h, outPixels, 0, w);
        return expandedMask;
    }

    private Rectangle expandRectangle(Rectangle bounds, int padding) {
        if (bounds == null || padding <= 0) {
            return bounds;
        }
        return new Rectangle(bounds.x - padding, bounds.y - padding, bounds.width + (padding * 2), bounds.height + (padding * 2));
    }


    private Rectangle getAlphaBounds(BufferedImage image) {
        int minX = image.getWidth();
        int minY = image.getHeight();
        int maxX = -1;
        int maxY = -1;

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) == 0) {
                    continue;
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (maxX < minX || maxY < minY) {
            return new Rectangle();
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    private void renderSinglePlane(CameraPlane plane) {
        CameraPositionPoint[] points = plane.getCornerPoints();
        if (points == null || points.length == 0) {
            return;
        }
        Color planeColor = new Color(plane.getColor());
        CameraTexCols[] texCols = plane.getTexCols();
        boolean texturedPlane = texCols != null && texCols.length > 0;
        PlaneStyle planeStyle = getPlaneStyle(plane, planeColor);

        Polygon p = new Polygon();
        p.addPoint(points[0].x, points[0].y);
        p.addPoint(points[1].x, points[1].y);
        p.addPoint(points[3].x, points[3].y);
        p.addPoint(points[2].x, points[2].y);

        boolean renderedTexture = false;
        if (texturedPlane) {
            renderedTexture = renderTexCols(p, plane, planeStyle);
        }

        if (!renderedTexture) {
            this.graphics.scale(1, 1);
            this.graphics.setBackground(planeStyle.fillColor());
            this.graphics.setStroke(simpleStroke);
            this.graphics.setColor(planeStyle.fillColor());
            this.graphics.fillPolygon(p);
            this.graphics.scale(1, 1);
        }
    }

    private int getRenderBackgroundColor() {
        CameraPlane backgroundPlane = getBackgroundPlane();
        if (backgroundPlane != null) {
            return backgroundPlane.getColor();
        }
        return this.backgroundColor;
    }

    private CameraPlane getBackgroundPlane() {
        if (data.getPlanes() == null || data.getPlanes().length == 0) {
            return null;
        }

        CameraPlane plane = data.getPlanes()[0];
        if (plane == null || hasTextureAssetWithPrefix(plane, "wall_texture") || hasTextureAssetWithPrefix(plane, "floor_texture")) {
            return null;
        }

        CameraPositionPoint[] points = plane.getCornerPoints();
        if (points == null || points.length < 4) {
            return null;
        }

        Polygon polygon = getPlanePolygon(points);
        Rectangle bounds = polygon.getBounds();
        return bounds.x <= 0 && bounds.y <= 0 && bounds.width >= this.width && bounds.height >= this.height ? plane : null;
    }

    private PlaneStyle getPlaneStyle(CameraPlane plane, Color planeColor) {
        if (isWallPlane(plane)) {
            Integer override = this.wallColorResolver.lookup(this.wallPaint);
            Color fill = (override != null) ? new Color(override) : planeColor;
            // neutralWall=false so the wallpaper texture (incl. _wall_color_* templates) renders;
            // tinting it with plane.color reproduces what the client paints.
            return new PlaneStyle(fill, fill, false);
        }

        CameraPlane topCapWall = findWallTopCapWallPlane(plane);
        if (topCapWall != null) {
            return new PlaneStyle(planeColor, planeColor, false);
        }

        return new PlaneStyle(planeColor, planeColor, false);
    }

    private CameraPlane findWallTopCapWallPlane(CameraPlane candidate) {
        if (candidate == null || isWallPlane(candidate) || isFloorPlane(candidate) || isLandscapePlane(candidate)) {
            return null;
        }

        CameraPositionPoint[] candidatePoints = candidate.getCornerPoints();
        if (candidatePoints == null || candidatePoints.length < 4) {
            return null;
        }

        if (!isThinPlaneEdge(candidatePoints[0], candidatePoints[1]) || !isThinPlaneEdge(candidatePoints[2], candidatePoints[3])) {
            return null;
        }

        for (CameraPlane wallPlane : data.getPlanes()) {
            if (wallPlane == null || !isWallPlane(wallPlane)) {
                continue;
            }

            CameraPositionPoint[] wallPoints = wallPlane.getCornerPoints();
            if (wallPoints == null || wallPoints.length < 4) {
                continue;
            }

            if (planeContainsPoint(candidatePoints, wallPoints[2]) && planeContainsPoint(candidatePoints, wallPoints[3])) {
                return wallPlane;
            }
        }

        return null;
    }

    private boolean planeContainsPoint(CameraPositionPoint[] points, CameraPositionPoint target) {
        if (points == null || target == null) {
            return false;
        }

        for (CameraPositionPoint point : points) {
            if (samePoint(point, target)) {
                return true;
            }
        }

        return false;
    }

    private boolean isThinPlaneEdge(CameraPositionPoint left, CameraPositionPoint right) {
        if (left == null || right == null) {
            return false;
        }

        int dx = left.x - right.x;
        int dy = left.y - right.y;
        return (dx * dx) + (dy * dy) <= 160;
    }

    private boolean samePoint(CameraPositionPoint left, CameraPositionPoint right) {
        return left != null && right != null && left.x == right.x && left.y == right.y;
    }

    private boolean isWallPlane(CameraPlane plane) {
        return hasTextureAssetWithPrefix(plane, "wall_texture");
    }

    private boolean isFloorPlane(CameraPlane plane) {
        return hasTextureAssetWithPrefix(plane, "floor_texture");
    }

    private boolean isLandscapePlane(CameraPlane plane) {
        return hasTextureAssetWithPrefix(plane, "landscape_");
    }

    private boolean hasTextureAssetWithPrefix(CameraPlane plane, String prefix) {
        if (plane.getTexCols() == null) {
            return false;
        }

        for (CameraTexCols texCol : plane.getTexCols()) {
            if (texCol == null || texCol.getAssetNames() == null) {
                continue;
            }

            for (String asset : texCol.getAssetNames()) {
                if (asset != null && asset.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Polygon getPlanePolygon(CameraPositionPoint[] points) {
        if (points == null || points.length < 4) {
            return new Polygon();
        }

        Polygon polygon = new Polygon();
        polygon.addPoint(points[0].x, points[0].y);
        polygon.addPoint(points[1].x, points[1].y);
        polygon.addPoint(points[3].x, points[3].y);
        polygon.addPoint(points[2].x, points[2].y);
        return polygon;
    }

    private boolean renderTexCols(Polygon polygon, CameraPlane plane, PlaneStyle planeStyle) {
        BufferedImage texture = buildPlaneTexture(plane, planeStyle);

        if (texture == null) {
            return false;
        }

        CameraPositionPoint[] points = plane.getCornerPoints();
        if (points == null || points.length < 4) {
            return false;
        }

        // Wall texture sprites are authored with the baseboard at the bottom of the source
        // image and the client paints walls in a mirrored frame on both axes (this is why
        // mask.x has to be mirrored to land the door correctly — RoomPlane.as ~505-506).
        // Without these flips the camera shows wall textures upside-down and asymmetric
        // patterns (chevrons, half-walls) come out reversed compared to the room view.
        if (isWallPlane(plane)) {
            texture = flipVertical(texture);
            texture = CameraUtils.flipHorizontal(texture);
        }

        AffineTransform oldTransform = this.graphics.getTransform();
        this.graphics.setClip(polygon);
        if (shouldPrefillTexturedPlane(plane)) {
            this.graphics.setColor(getTexturePrefillColor(plane, planeStyle));
            this.graphics.fillPolygon(polygon);
        }
        AffineTransform transform = getTextureTransform(plane, texture.getWidth(), texture.getHeight());
        if (transform == null) {
            this.graphics.setClip(null);
            this.graphics.setTransform(oldTransform);
            return false;
        }
        this.graphics.drawImage(texture, transform, null);
        this.graphics.setClip(null);
        this.graphics.setTransform(oldTransform);
        return true;
    }

    private AffineTransform getTextureTransform(CameraPlane plane, int textureWidth, int textureHeight) {
        CameraPositionPoint[] points = plane.getCornerPoints();
        if (points == null || points.length < 4 || textureWidth <= 0 || textureHeight <= 0) {
            return null;
        }

        CameraPositionPoint widthPoint = getTextureWidthPoint(plane, points);
        CameraPositionPoint heightPoint = getTextureHeightPoint(plane, points);
        return new AffineTransform(
                (widthPoint.x - points[0].x) / (double) textureWidth,
                (widthPoint.y - points[0].y) / (double) textureWidth,
                (heightPoint.x - points[0].x) / (double) textureHeight,
                (heightPoint.y - points[0].y) / (double) textureHeight,
                points[0].x,
                points[0].y
        );
    }

    private boolean shouldPrefillTexturedPlane(CameraPlane plane) {
        if (isWallPlane(plane) && hasPlaneMasks(plane)) {
            return true;
        }

        boolean hasBitmapMasks = plane.getMasks() != null && plane.getMasks().length > 0;
        boolean hasRectangleMasks = plane.getRectangleMasks() != null && plane.getRectangleMasks().length > 0;
        return !hasBitmapMasks && !hasRectangleMasks;
    }

    private Color getTexturePrefillColor(CameraPlane plane, PlaneStyle planeStyle) {
        if (isWallPlane(plane) && hasPlaneMasks(plane)) {
            // Masked walls are redrawn and the doorway is punched back out later, so using
            // the wall fill here prevents 1px background leaks along the transformed wall edge.
            return planeStyle.fillColor();
        }

        return planeStyle.fillColor();
    }

    private boolean hasPlaneMasks(CameraPlane plane) {
        return (plane.getMasks() != null && plane.getMasks().length > 0)
                || (plane.getRectangleMasks() != null && plane.getRectangleMasks().length > 0);
    }

    private BufferedImage buildPlaneTexture(CameraPlane plane, PlaneStyle planeStyle) {
        Rectangle bounds = getPlaneTextureBounds(plane);
        if (bounds.width <= 0 || bounds.height <= 0) {
            return null;
        }

        BufferedImage texture = new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D textureGraphics = texture.createGraphics();
        textureGraphics.setComposite(AlphaComposite.Src);

        List<BufferedImage> columns = new ArrayList<>();
        for (CameraTexCols texCol : plane.getTexCols()) {
            BufferedImage column = buildTextureColumn(texCol, bounds.height, plane.isBottomAligned(), planeStyle, plane.getTextureOffsetX(), plane.getTextureOffsetY());
            if (column != null) {
                columns.add(column);
            }
        }

        if (columns.isEmpty()) {
            textureGraphics.dispose();
            return null;
        }

        for (int x = 0; x < bounds.width; ) {
            int startX = x;
            for (BufferedImage column : columns) {
                textureGraphics.drawImage(column, x, 0, null);
                x += column.getWidth();
                if (x >= bounds.width) {
                    break;
                }
            }
            if (x == startX) {
                break;
            }
        }

        textureGraphics.dispose();
        applyMasks(texture, plane.getMasks());
        applyRectangleMasks(texture, plane.getRectangleMasks());
        return texture;
    }

    private Rectangle getPlaneTextureBounds(CameraPlane plane) {
        CameraPositionPoint[] points = plane.getCornerPoints();
        if (points == null || points.length < 4) {
            return new Rectangle();
        }

        CameraPositionPoint widthPoint = getTextureWidthPoint(plane, points);
        CameraPositionPoint heightPoint = getTextureHeightPoint(plane, points);
        int width = Math.max(1, Math.abs(points[0].x - widthPoint.x));
        int height;
        if (isWallPlane(plane)) {
            height = Math.max(1, Math.abs(points[0].y - heightPoint.y));
        } else {
            // For floors and floor side-faces, take the larger of the X and Y projections of
            // the height-axis. The X-only shortcut works for the main floor rhombus (X and Y
            // are similar in iso projection) but collapses to 1px for thin south-east/west
            // lip strips whose height-axis corner shares X with the anchor — that's why the
            // floor texture wasn't wrapping around the lip and showed a solid colour instead.
            int hX = Math.abs(points[0].x - heightPoint.x);
            int hY = Math.abs(points[0].y - heightPoint.y);
            height = Math.max(1, Math.max(hX, hY));
        }
        return new Rectangle(0, 0, width, height);
    }

    private CameraPositionPoint getTextureWidthPoint(CameraPlane plane, CameraPositionPoint[] points) {
        return points[1];
    }

    private CameraPositionPoint getTextureHeightPoint(CameraPlane plane, CameraPositionPoint[] points) {
        return points[2];
    }

    private BufferedImage buildTextureColumn(CameraTexCols texCol, int height, boolean bottomAligned, PlaneStyle planeStyle, int textureOffsetX, int textureOffsetY) {
        if (texCol == null || texCol.getAssetNames() == null || texCol.getAssetNames().length == 0) {
            return null;
        }

        List<BufferedImage> cells = new ArrayList<>();
        boolean repeat = true;
        int width = 0;
        int naturalHeight = 0;

        for (String asset : texCol.getAssetNames()) {
            if (asset == null || asset.isEmpty()) {
                repeat = false;
                continue;
            }

            if (shouldSkipNeutralWallTexture(asset, planeStyle)) {
                continue;
            }

            BufferedImage cell = readSprite(asset);
            if (cell == null) {
                continue;
            }

            cell = offsetTextureCell(cell, textureOffsetX, textureOffsetY);
            cell = tintTexture(cell, planeStyle.textureTint());
            cells.add(cell);
            width = Math.max(width, cell.getWidth());
            naturalHeight += cell.getHeight();
        }

        if (cells.isEmpty() || width <= 0) {
            return null;
        }

        int columnHeight = repeat ? height : Math.min(height, Math.max(1, naturalHeight));
        BufferedImage column = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D columnGraphics = column.createGraphics();
        columnGraphics.setComposite(AlphaComposite.SrcOver);

        int y = bottomAligned ? height - columnHeight : 0;
        int endY = y + columnHeight;
        while (y < endY) {
            int startY = y;
            for (BufferedImage cell : cells) {
                int x = (width - cell.getWidth()) / 2;
                columnGraphics.drawImage(cell, x, y, null);
                y += cell.getHeight();
                if (y >= endY || !repeat) {
                    break;
                }
            }
            if (y == startY || !repeat) {
                break;
            }
        }

        columnGraphics.dispose();
        return column;
    }

    private BufferedImage offsetTextureCell(BufferedImage source, int textureOffsetX, int textureOffsetY) {
        if (source.getWidth() <= 0 || source.getHeight() <= 0 || (textureOffsetX == 0 && textureOffsetY == 0)) {
            return source;
        }

        int offsetX = positiveModulo(textureOffsetX, source.getWidth());
        int offsetY = positiveModulo(textureOffsetY, source.getHeight());
        if (offsetX == 0 && offsetY == 0) {
            return source;
        }

        BufferedImage shifted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < shifted.getWidth(); x++) {
            int sourceX = (x + offsetX) % source.getWidth();
            for (int y = 0; y < shifted.getHeight(); y++) {
                int sourceY = (y + offsetY) % source.getHeight();
                shifted.setRGB(x, y, source.getRGB(sourceX, sourceY));
            }
        }
        return shifted;
    }

    private int positiveModulo(int value, int divisor) {
        int result = value % divisor;
        return result < 0 ? result + divisor : result;
    }

    private boolean shouldSkipNeutralWallTexture(String asset, PlaneStyle planeStyle) {
        return planeStyle.neutralWall() && asset.startsWith("wall_texture") && asset.contains("_wall_color_");
    }

    private BufferedImage readSprite(String asset) {
        if (asset == null || asset.isEmpty()) {
            return null;
        }
        if (this.spriteCache.containsKey(asset)) {
            return this.spriteCache.get(asset);
        }

        BufferedImage image = null;
        File file = spriteFile(asset);
        if (file != null) {
            try {
                image = ImageIO.read(file);
            } catch (Exception e) {
                image = null;
            }
        }
        this.spriteCache.put(asset, image);
        return image;
    }

    private BufferedImage tintTexture(BufferedImage source, Color tint) {
        int w = source.getWidth();
        int h = source.getHeight();
        int[] pixels = new int[w * h];
        source.getRGB(0, 0, w, h, pixels, 0, w);

        float tintR = tint.getRed() / 255f;
        float tintG = tint.getGreen() / 255f;
        float tintB = tint.getBlue() / 255f;

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int alpha = (pixel >>> 24) & 0xFF;
            if (alpha == 0) {
                pixels[i] = 0;
                continue;
            }
            int red = Math.round(((pixel >> 16) & 0xFF) * tintR);
            int green = Math.round(((pixel >> 8) & 0xFF) * tintG);
            int blue = Math.round((pixel & 0xFF) * tintB);
            pixels[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }

        BufferedImage tinted = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        tinted.setRGB(0, 0, w, h, pixels, 0, w);
        return tinted;
    }

    private void applyMasks(BufferedImage texture, CameraMask[] masks) {
        if (masks == null || masks.length == 0) {
            return;
        }

        for (CameraMask mask : masks) {
            if (mask == null || mask.getLocation() == null || mask.getName() == null || mask.getName().isEmpty()) {
                continue;
            }

            BufferedImage maskImage = readSprite(mask.getName());
            if (maskImage == null) {
                continue;
            }

            if (mask.isFlipH()) {
                maskImage = CameraUtils.flipHorizontal(maskImage);
            }
            if (mask.isFlipV()) {
                maskImage = flipVertical(maskImage);
            }

            // The client computes wall mask coordinates in a mirrored frame on both axes
            // (RoomPlane.as ~505-506). We reproduce that frame implicitly by flipping the
            // whole wall texture H+V in renderTexCols, which carries the mask cutouts along
            // with it — so the cutouts placed at raw (location.x, location.y) here end up
            // at the same screen position the room view paints them.
            clearMaskedPixels(texture, maskImage, mask.getLocation().x, mask.getLocation().y);
        }
    }

    private void applyRectangleMasks(BufferedImage texture, CameraRectangleMask[] masks) {
        if (masks == null || masks.length == 0) {
            return;
        }

        for (CameraRectangleMask mask : masks) {
            if (mask == null || mask.getWidth() <= 0 || mask.getHeight() <= 0) {
                continue;
            }

            int startX = Math.max(0, mask.getX());
            int startY = Math.max(0, mask.getY());
            int endX = Math.min(texture.getWidth(), mask.getX() + mask.getWidth());
            int endY = Math.min(texture.getHeight(), mask.getY() + mask.getHeight());
            int rectW = endX - startX;
            int rectH = endY - startY;
            if (rectW <= 0 || rectH <= 0) {
                continue;
            }

            int[] zeros = new int[rectW * rectH];
            texture.setRGB(startX, startY, rectW, rectH, zeros, 0, rectW);
        }
    }

    private void clearMaskedPixels(BufferedImage texture, BufferedImage mask, int startX, int startY) {
        int maskW = mask.getWidth();
        int maskH = mask.getHeight();
        int textureW = texture.getWidth();
        int textureH = texture.getHeight();

        int xMin = Math.max(0, -startX);
        int yMin = Math.max(0, -startY);
        int xMax = Math.min(maskW, textureW - startX);
        int yMax = Math.min(maskH, textureH - startY);
        int rw = xMax - xMin;
        int rh = yMax - yMin;
        if (rw <= 0 || rh <= 0) {
            return;
        }

        int[] maskPixels = new int[rw * rh];
        mask.getRGB(xMin, yMin, rw, rh, maskPixels, 0, rw);

        int destX = startX + xMin;
        int destY = startY + yMin;
        int[] texturePixels = new int[rw * rh];
        texture.getRGB(destX, destY, rw, rh, texturePixels, 0, rw);

        boolean changed = false;
        for (int i = 0; i < maskPixels.length; i++) {
            if (((maskPixels[i] >>> 24) & 0xFF) > 0) {
                texturePixels[i] = 0;
                changed = true;
            }
        }

        if (changed) {
            texture.setRGB(destX, destY, rw, rh, texturePixels, 0, rw);
        }
    }

    private BufferedImage flipVertical(BufferedImage image) {
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -image.getHeight(null));
        BufferedImage flipped = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = flipped.createGraphics();
        graphics.drawImage(image, tx, null);
        graphics.dispose();
        return flipped;
    }

    private void renderSingleSprite(CameraSprite sprite) {
        renderSprite(this.graphics, sprite);
    }

    private void renderSinglePlaneClippedToMask(CameraPlane plane, DoorwayMask doorwayMask) {
        BufferedImage planeLayer = renderPlaneLayer(plane);
        if (planeLayer == null) {
            return;
        }

        drawMaskedLayer(planeLayer, doorwayMask);
    }

    private void renderSingleSpriteClippedToMask(CameraSprite sprite, DoorwayMask doorwayMask) {
        BufferedImage spriteLayer = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D spriteGraphics = spriteLayer.createGraphics();
        applyPixelRenderingHints(spriteGraphics);
        renderSprite(spriteGraphics, sprite);
        spriteGraphics.dispose();

        drawMaskedLayer(spriteLayer, doorwayMask);
    }

    private BufferedImage renderPlaneLayer(CameraPlane plane) {
        BufferedImage planeLayer = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D planeGraphics = planeLayer.createGraphics();
        applyPixelRenderingHints(planeGraphics);

        Graphics2D previousGraphics = this.graphics;
        this.graphics = planeGraphics;
        try {
            renderSinglePlane(plane);
        } finally {
            this.graphics = previousGraphics;
            planeGraphics.dispose();
        }

        return planeLayer;
    }

    private void drawMaskedLayer(BufferedImage layer, DoorwayMask doorwayMask) {
        Rectangle bounds = doorwayMask.bounds;
        int x0 = Math.max(0, bounds.x);
        int y0 = Math.max(0, bounds.y);
        int x1 = Math.min(this.width, bounds.x + bounds.width);
        int y1 = Math.min(this.height, bounds.y + bounds.height);
        int rw = x1 - x0;
        int rh = y1 - y0;
        if (rw <= 0 || rh <= 0) {
            return;
        }

        int[] layerPixels = new int[rw * rh];
        layer.getRGB(x0, y0, rw, rh, layerPixels, 0, rw);
        int[] maskPixels = new int[rw * rh];
        doorwayMask.alpha.getRGB(x0, y0, rw, rh, maskPixels, 0, rw);

        int[] clippedPixels = new int[rw * rh];
        for (int i = 0; i < layerPixels.length; i++) {
            int layerPixel = layerPixels[i];
            int layerAlpha = (layerPixel >>> 24) & 0xFF;
            if (layerAlpha == 0) {
                continue;
            }
            int maskAlpha = (maskPixels[i] >>> 24) & 0xFF;
            if (maskAlpha == 0) {
                continue;
            }
            int alpha = (layerAlpha * maskAlpha) / 255;
            clippedPixels[i] = (layerPixel & 0x00FFFFFF) | (alpha << 24);
        }

        BufferedImage clippedLayer = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
        clippedLayer.setRGB(x0, y0, rw, rh, clippedPixels, 0, rw);
        this.graphics.drawImage(clippedLayer, 0, 0, null);
    }

    private void renderSprite(Graphics2D target, CameraSprite sprite) {
        int alpha = (sprite.getAlpha() != 0) ? sprite.getAlpha() : 255;
        if (alpha <= 0 || alpha > 255) {
            alpha = 1;
        }

        BufferedImage spriteImage = sprite.isFromUrl()
                ? readUrlImage(sprite.getName())
                : readSprite(sprite.getName());

        if (spriteImage == null) {
            return;
        }

        if (sprite.isFlipH()) {
            spriteImage = CameraUtils.flipHorizontal(spriteImage);
        }

        if (this.paletteCache != null && !sprite.getPaletteSourceName().isEmpty()) {
            spriteImage = copyBufferedImage(spriteImage);
            CameraPalette petPalette = this.paletteCache.getPalette(sprite.getPaletteSourceName());

            if (petPalette != null) {
                int[] alphaArray = new int[256];
                CameraUtils.paletteMap(spriteImage, alphaArray, petPalette.rgb, alphaArray, alphaArray);
            }
        }

        if (sprite.getColor() != 16777215 && sprite.getColor() != -1) {
            spriteImage = copyBufferedImage(spriteImage);
            CameraUtils.recolor(spriteImage, new Color((0xFF << 24) | sprite.getColor()));
        }

        Composite previousComposite = target.getComposite();
        if (sprite.getBlendMode().equalsIgnoreCase("add")) {
            target.setComposite(new AdditiveComposite());
        } else {
            target.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha / 255.0f));
        }

        if (sprite.getSkew() != 0) {
            AffineTransform oldTransform = target.getTransform();
            AffineTransform transform = AffineTransform.getShearInstance(0, 0);
            transform.shear(0, sprite.isFlipH() ? sprite.getSkew() : -sprite.getSkew());
            target.setTransform(transform);
            target.drawImage(spriteImage, sprite.getX(), sprite.getY() + (spriteImage.getHeight() / 2), null);
            target.setTransform(oldTransform);
        } else {
            target.drawImage(spriteImage, sprite.getX(), sprite.getY(), null);
        }

        target.setComposite(previousComposite);
    }

    private BufferedImage copyBufferedImage(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private void applyPixelRenderingHints(Graphics2D target) {
        target.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        target.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        target.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        target.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        target.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }

    private static BufferedImage readUrlImage(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.startsWith("//")) {
            name = "http:" + name;
        }
        try {
            URL url = new URL(name);
            if (!isAllowedSpriteUrlScheme(url.getProtocol())) {
                LOGGER.debug("Rejecting non-http(s) camera sprite URL: {}", name);
                return null;
            }
            if (!isAllowedSpriteUrlHost(url.getHost())) {
                LOGGER.debug("Rejecting private/loopback camera sprite URL: {}", name);
                return null;
            }
            URLConnection conn = url.openConnection();
            if (conn instanceof HttpURLConnection httpConn) {
                // Don't follow redirects — a 30x to an internal address would bypass the
                // host check above. Caller treats null as "couldn't fetch".
                httpConn.setInstanceFollowRedirects(false);
            }
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            try (InputStream stream = conn.getInputStream()) {
                return ImageIO.read(stream);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch camera sprite from URL: {}", name, e);
            return null;
        }
    }

    private static boolean isAllowedSpriteUrlScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private static boolean isAllowedSpriteUrlHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (address.isAnyLocalAddress()
                        || address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isMulticastAddress()) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private void renderFilters() {
        if (data.getFilters() == null) {
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

                float red   = matrix[0]  * srcR + matrix[1]  * srcG + matrix[2]  * srcB + matrix[3]  * srcA + matrix[4]  * filterAlphaTerm;
                float green = matrix[5]  * srcR + matrix[6]  * srcG + matrix[7]  * srcB + matrix[8]  * srcA + matrix[9]  * filterAlphaTerm;
                float blue  = matrix[10] * srcR + matrix[11] * srcG + matrix[12] * srcB + matrix[13] * srcA + matrix[14] * filterAlphaTerm;
                float a     = matrix[15] * srcR + matrix[16] * srcG + matrix[17] * srcB + matrix[18] * srcA + matrix[19] * filterAlphaTerm;

                int rOut = clampByte(red);
                int gOut = clampByte(green);
                int bOut = clampByte(blue);
                int aOut = clampByte(a);

                pixels[i] = (aOut << 24) | (rOut << 16) | (gOut << 8) | bOut;
            }

            this.render.setRGB(0, 0, w, h, pixels, 0, w);
        }
    }

    private static int clampByte(float v) {
        if (v <= 0f) return 0;
        if (v >= 255f) return 255;
        return Math.round(v);
    }

    private void renderEffects() {
        if (data.getFilters() == null) {
            return;
        }

        for (CameraFilter modifier : this.data.getFilters()) {
            CameraFilters filter = CameraFilters.fromName(modifier.getName());

            if (filter == null || filter.filterType != CameraFilters.FilterType.COMPOSITE)
                continue;

            File effectFile = spriteFile(modifier.getName());
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

    private static final class AvatarSpriteCluster {
        private final List<CameraSprite> sprites = new ArrayList<>();
        private final Rectangle anchorBounds;
        private final double anchorDepth;
        private Rectangle bounds = null;
        private double maxDepth = Double.NEGATIVE_INFINITY;

        private AvatarSpriteCluster(CameraSprite anchorSprite, Rectangle anchorBounds) {
            this.anchorBounds = anchorBounds == null ? null : new Rectangle(anchorBounds);
            this.anchorDepth = anchorSprite == null ? Double.NaN : anchorSprite.getZ();
        }

        private void add(CameraSprite sprite, Rectangle spriteBounds) {
            this.sprites.add(sprite);
            this.maxDepth = Math.max(this.maxDepth, sprite.getZ());
            if (spriteBounds == null || spriteBounds.isEmpty()) {
                return;
            }

            if (this.bounds == null) {
                this.bounds = new Rectangle(spriteBounds);
            } else {
                this.bounds = this.bounds.union(spriteBounds);
            }
        }

        private boolean intersects(Rectangle other) {
            return this.bounds != null && other != null && this.bounds.intersects(other);
        }

        private List<CameraSprite> sprites() {
            return this.sprites;
        }

        private boolean hasSpriteMatching(java.util.function.Predicate<CameraSprite> predicate) {
            for (CameraSprite sprite : this.sprites) {
                if (predicate.test(sprite)) {
                    return true;
                }
            }
            return false;
        }

        private Rectangle assignmentBounds() {
            return this.bounds != null ? this.bounds : this.anchorBounds;
        }

        private Rectangle doorwayFootprintBounds() {
            Rectangle sourceBounds = assignmentBounds();
            if (sourceBounds == null || sourceBounds.isEmpty()) {
                return sourceBounds;
            }

            int footprintY = sourceBounds.y + (int) Math.floor(sourceBounds.height * 0.6);
            int footprintHeight = Math.max(1, (sourceBounds.y + sourceBounds.height) - footprintY);
            return new Rectangle(sourceBounds.x, footprintY, sourceBounds.width, footprintHeight);
        }

        private double referenceDepth() {
            return Double.isNaN(this.anchorDepth) ? this.maxDepth : this.anchorDepth;
        }
    }

    private record DoorwayMask(BufferedImage alpha, Rectangle bounds) {
    }

    private record PlaneStyle(Color fillColor, Color textureTint, boolean neutralWall) {
    }
}
