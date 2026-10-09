package net.nyalkan.config;

import net.nyalkan.Initializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.nyalkan.config.video.VideoModeManager;
import net.nyalkan.config.video.VideoModeSet;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public class Config {

    public int frameQueueSize = 2;
    public VideoModeSet.VideoMode videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
    public boolean windowedFullscreen = false;

    public int advCulling = 2;
    public boolean indirectDraw = true;

    public boolean uniqueOpaqueLayer = true;
    public boolean entityCulling = true;
    public boolean immediateBatching = true;
    public boolean animateOnlyVisible = true;
    public boolean poseReuse = true;
    public boolean modelReplay = true;
    public boolean blockEntityReplay = true;
    public boolean entityReplay = true;
    public boolean occlusionCulling = true;
    public boolean tickSpreading = true;
    public boolean animationLod = true;
    public boolean hudBatching = true;
    public boolean hudCaching = true;
    public boolean smoothHudUpdates = true;
    public int hudLowFpsRate = 80;
    public boolean fpsOverlay = false;
    public boolean entityBackfaceCulling = true;
    public boolean gpuParticles = true;
    public boolean bakedBlockEntities = true;
    public boolean mipmapSpriteFix = true;
    public int device = -1;

    public int ambientOcclusion = 1;

    public void write() {

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(CONFIG_PATH, Collections.singleton(GSON.toJson(this)));
        } catch (IOException e) {
            Initializer.LOGGER.error("Could not save settings", e);
        }
    }

    private static Path CONFIG_PATH;

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static Config load(Path path) {
        Config config;
        Config.CONFIG_PATH = path;

        if (Files.exists(path)) {
            try (FileReader fileReader = new FileReader(path.toFile())) {
                config = GSON.fromJson(fileReader, Config.class);
            }
            catch (IOException exception) {
                throw new RuntimeException(exception.getMessage());
            }
        }
        else {
            config = new Config();
            config.write();
        }

        return config;
    }
}
