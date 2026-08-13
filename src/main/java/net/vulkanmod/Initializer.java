package net.vulkanmod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.vulkanmod.config.Config;
import net.vulkanmod.config.Platform;
import net.vulkanmod.config.video.VideoModeManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

@Mod(value = "vulkanmod", dist = Dist.CLIENT)
public class Initializer {
	public static final Logger LOGGER = LogManager.getLogger("VulkanMod");

	private static String VERSION;

	// set before mod construction
	public static final Config CONFIG;

	static {
		System.setProperty("org.lwjgl.system.MemoryStack.SIZE", "1024K");
		org.lwjgl.system.Configuration.STACK_SIZE.set(1024 * 1024);

		Platform.init();
		VideoModeManager.init();

		Path configPath = FMLPaths.CONFIGDIR.get().resolve("vulkanmod_settings.json");
		CONFIG = loadConfig(configPath);
	}

	public Initializer(ModContainer modContainer) {
		VERSION = modContainer.getModInfo().getVersion().toString();

		LOGGER.info("== VulkanMod (NeoForge) ==");
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