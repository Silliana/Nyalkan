package net.nyalkan;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.nyalkan.config.Config;
import net.nyalkan.config.Platform;
import net.nyalkan.config.video.VideoModeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(value = "nyalkan", dist = Dist.CLIENT)
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("Nyalkan");

	private static String VERSION;

	// set before mod construction
	public static final Config CONFIG;

	static {
		System.setProperty("org.lwjgl.system.MemoryStack.SIZE", "1024K");
		org.lwjgl.system.Configuration.STACK_SIZE.set(1024 * 1024);

		Platform.init();
		VideoModeManager.init();

		Path configPath = FMLPaths.CONFIGDIR.get().resolve("nyalkan_settings.json");
		// keeps pre rename settings
		Path oldPath = FMLPaths.CONFIGDIR.get().resolve("vulkanmod_settings.json");
		if (Files.notExists(configPath) && Files.exists(oldPath)) {
			try {
				Files.copy(oldPath, configPath);
			} catch (IOException e) {
				LOGGER.warn("Could not copy old settings", e);
			}
		}
		CONFIG = loadConfig(configPath);
	}

	public Initializer(ModContainer modContainer) {
		VERSION = modContainer.getModInfo().getVersion().toString();

		LOGGER.info("== Nyalkan (NeoForge) ==");
	}

	private static Config loadConfig(Path path) {
		Config config = Config.load(path);

		if (config == null) {
			config = new Config();
			config.write();
		}

		return config;
	}

	public static String getVersion() {
		return VERSION;
	}
}