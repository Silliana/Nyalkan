package net.vulkanmod.mixin.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.SystemInfo;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.Device;
import net.vulkanmod.vulkan.memory.MemoryManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static net.vulkanmod.Initializer.getVersion;

@Mixin(DebugScreenOverlay.class)
public abstract class DebugScreenOverlayM {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private static long bytesToMegabytes(long bytes) {
        return 0;
    }

    @Shadow
    @Final
    private Font font;

    @Shadow
    protected abstract List<String> getGameInformation();

    @Shadow
    protected abstract List<String> getSystemInformation();

    // throttled jmx queries
    private static List<String> cachedSystemInfo = new ArrayList<>();
    private static long lastSystemInfoUpdate = 0;
    private static final long SYSTEM_INFO_INTERVAL_MS = 200;

    @Redirect(method = "getSystemInformation", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList([Ljava/lang/Object;)Ljava/util/ArrayList;"))
    private ArrayList<String> redirectList(Object[] elements) {
        long now = System.currentTimeMillis();
        if (now - lastSystemInfoUpdate < SYSTEM_INFO_INTERVAL_MS) {
            return new ArrayList<>(cachedSystemInfo);
        }
        lastSystemInfoUpdate = now;

        try {
            ArrayList<String> strings = new ArrayList<>();

            long maxMemory = Runtime.getRuntime().maxMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            long freeMemory = Runtime.getRuntime().freeMemory();
            long usedMemory = totalMemory - freeMemory;

            Device device = Vulkan.getDevice();

            strings.add(String.format("Java: %s", System.getProperty("java.version")));
            strings.add(String.format("Mem: % 2d%% %03d/%03dMB", usedMemory * 100L / maxMemory, bytesToMegabytes(usedMemory), bytesToMegabytes(maxMemory)));
            strings.add(String.format("Allocated: % 2d%% %03dMB", totalMemory * 100L / maxMemory, bytesToMegabytes(totalMemory)));
            strings.add(String.format("Off-heap: " + getOffHeapMemory() + "MB"));
            strings.add("NativeMemory: %dMB".formatted(MemoryManager.getInstance().getNativeMemoryMB()));
            strings.add("DeviceMemory: %dMB".formatted(MemoryManager.getInstance().getAllocatedDeviceMemoryMB()));
            strings.add("");
            strings.add("VulkanMod " + getVersion());
            strings.add("CPU: " + SystemInfo.cpuInfo);
            strings.add("GPU: " + device.deviceName);
            strings.add("Driver: " + device.driverVersion);
            strings.add("Vulkan: " + device.vkVersion);
            strings.add("");
            strings.add("");

            if (WorldRenderer.getInstance() != null && WorldRenderer.getInstance().getChunkAreaManager() != null) {
                Collections.addAll(strings, WorldRenderer.getInstance().getChunkAreaManager().getStats());
            }

            cachedSystemInfo = strings;
            return strings;
        } catch (Throwable t) {
            net.vulkanmod.Initializer.LOGGER.error("[VulkanMod] F3 system information failed to build", t);
            ArrayList<String> empty = new ArrayList<>();
            cachedSystemInfo = empty;
            return empty;
        }
    }

    private long getOffHeapMemory() {
        return bytesToMegabytes(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed());
    }
}
