package com.eu.habbo.imaging.camera.render;

import com.google.gson.Gson;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RenderFromJsonMain {
    public static void main(String[] args) throws Exception {
        Path jsonPath = Paths.get(args.length > 0 ? args[0] : "debug/scene.json");
        Path outPath = Paths.get(args.length > 1 ? args[1] : "debug/out.png");
        Path spritesDir = Paths.get(args.length > 2 ? args[2] : "C:/habbo/camera-tool/out/sprites");

        String json = Files.readString(jsonPath);
        JSONCamera scene = new Gson().fromJson(json, JSONCamera.class);

        WallColorResolver resolver = new WallColorResolver(null);
        int backgroundColor = 0x000000;
        BufferedImage image = new CameraRenderImage(scene, backgroundColor, spritesDir, null, null, resolver).render();

        File outFile = outPath.toFile();
        File parent = outFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        ImageIO.write(image, "png", outFile);
        System.out.println("Rendered " + image.getWidth() + "x" + image.getHeight() + " -> " + outFile.getAbsolutePath());
    }
}
