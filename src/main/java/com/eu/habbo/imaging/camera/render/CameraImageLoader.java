package com.eu.habbo.imaging.camera.render;

import com.eu.habbo.Emulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class CameraImageLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CameraImageLoader.class);

    private final Path spritesDir;
    private final Path framesDir;
    private final Map<String, BufferedImage> spriteCache = new HashMap<>();
    private final Map<String, BufferedImage> frameCache = new HashMap<>();
    private final Map<String, BufferedImage> urlImageCache = new HashMap<>();

    private FetchBudget fetchBudget;

    CameraImageLoader(Path spritesDir, Path framesDir) {
        this.spritesDir = spritesDir;
        this.framesDir = framesDir;
    }

    void resetFetchBudget(int maxFetches, int budgetMs) {
        this.fetchBudget = new FetchBudget(maxFetches, budgetMs);
    }

    File spriteFile(String asset) {
        return resolveAssetFile(this.spritesDir, asset);
    }

    File frameFile(String asset) {
        return resolveAssetFile(this.framesDir, asset);
    }

    private static File resolveAssetFile(Path dir, String asset) {
        if (asset == null || asset.isEmpty()) {
            return null;
        }
        // Normalize and confirm the resolved path stays inside dir, so an asset
        // name from JSON like "../../config/server" can't escape the assets directory.
        Path resolved = dir.resolve(asset + ".png").normalize();
        if (!resolved.startsWith(dir)) {
            return null;
        }
        return resolved.toFile();
    }

    BufferedImage readSprite(String asset) {
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
        // null is cached intentionally: a failed read should not be retried on the same render
        this.spriteCache.put(asset, image);
        return image;
    }

    BufferedImage readFrame(String asset) {
        if (asset == null || asset.isEmpty()) {
            return null;
        }
        if (this.frameCache.containsKey(asset)) {
            return this.frameCache.get(asset);
        }

        BufferedImage image = null;
        File file = frameFile(asset);
        if (file != null) {
            try {
                image = ImageIO.read(file);
            } catch (Exception e) {
                image = null;
            }
        }
        // null is cached intentionally: a failed read should not be retried on the same render
        this.frameCache.put(asset, image);
        return image;
    }

    BufferedImage readUrlImage(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (this.urlImageCache.containsKey(name)) {
            return this.urlImageCache.get(name);
        }
        if (this.fetchBudget == null || !this.fetchBudget.tryConsume()) {
            LOGGER.debug("Camera URL fetch budget exhausted, skipping: {}", name);
            return null;
        }

        BufferedImage image = fetchUrlImage(name);
        this.urlImageCache.put(name, image);
        return image;
    }

    BufferedImage getSpriteImage(CameraSprite sprite) {
        if (sprite == null) {
            return null;
        }

        return sprite.isFromUrl() ? readUrlImage(sprite.getName()) : readSprite(sprite.getName());
    }

    private static BufferedImage fetchUrlImage(String name) {
        String requestUrl = name.startsWith("//") ? "http:" + name : name;
        try {
            URL url = new URL(requestUrl);
            if (!isAllowedSpriteUrlScheme(url.getProtocol())) {
                LOGGER.debug("Rejecting non-http(s) camera sprite URL: {}", requestUrl);
                return null;
            }
            if (!isAllowedSpriteUrlHost(url.getHost())) {
                LOGGER.debug("Rejecting private/loopback camera sprite URL: {}", requestUrl);
                return null;
            }
            URLConnection conn = url.openConnection();
            if (conn instanceof HttpURLConnection httpConn) {
                // Don't follow redirects â€” a 30x to an internal address would bypass the
                // host check above. Caller treats null as "couldn't fetch".
                httpConn.setInstanceFollowRedirects(false);
            }
            conn.setConnectTimeout(Emulator.getConfig().getInt("camera.image.fetch.connect.timeout.ms", 2000));
            conn.setReadTimeout(Emulator.getConfig().getInt("camera.image.fetch.read.timeout.ms", 3000));
            int maxBytes = Emulator.getConfig().getInt("camera.image.fetch.max.bytes", 2097152);
            try (InputStream stream = conn.getInputStream()) {
                byte[] bytes = readBounded(stream, maxBytes);
                if (bytes == null) {
                    LOGGER.debug("Camera sprite URL exceeded max bytes ({}): {}", maxBytes, requestUrl);
                    return null;
                }
                return ImageIO.read(new ByteArrayInputStream(bytes));
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to fetch camera sprite from URL: {}", requestUrl, e);
            return null;
        }
    }

    private static byte[] readBounded(InputStream stream, int maxBytes) throws java.io.IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(Math.min(maxBytes, 16384));
        byte[] chunk = new byte[8192];
        int total = 0;
        int read;
        while ((read = stream.read(chunk)) != -1) {
            if (total + read > maxBytes) {
                return null;
            }
            buffer.write(chunk, 0, read);
            total += read;
        }
        return buffer.toByteArray();
    }

    private static boolean isAllowedSpriteUrlScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private static boolean isAllowedSpriteUrlHost(String host) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        if (isHostAllowlisted(host)) {
            return true;
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

    private static boolean isHostAllowlisted(String host) {
        String configured = Emulator.getConfig().getValue("camera.allowed.image.hosts", "");
        if (configured.isEmpty()) {
            return false;
        }
        String target = host.toLowerCase(Locale.ROOT);
        for (String entry : configured.split(",")) {
            String trimmed = entry.trim().toLowerCase(Locale.ROOT);
            if (trimmed.isEmpty() || trimmed.equals("*")) {
                continue;
            }
            if (trimmed.startsWith("*.")) {
                String suffix = trimmed.substring(2);
                if (suffix.isEmpty()) {
                    continue;
                }
                if (target.equals(suffix) || target.endsWith("." + suffix)) {
                    return true;
                }
            } else if (trimmed.equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static final class FetchBudget {
        private int remaining;
        private final long deadlineNanos;

        FetchBudget(int maxFetches, int budgetMs) {
            this.remaining = maxFetches;
            this.deadlineNanos = System.nanoTime() + budgetMs * 1_000_000L;
        }

        boolean tryConsume() {
            if (this.remaining <= 0 || System.nanoTime() > this.deadlineNanos) {
                return false;
            }
            this.remaining--;
            return true;
        }
    }
}
