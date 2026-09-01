package net.nyalkan.mixin.debug;

import com.mojang.blaze3d.platform.GlUtil;
import net.nyalkan.vulkan.SystemInfo;
import net.nyalkan.vulkan.Vulkan;
import net.nyalkan.vulkan.device.Device;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GlUtil.class)
public class GlDebugInfoM {

    @Overwrite
    public static String getVendor() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().vendorIdString : "n/a";
    }

    @Overwrite
    public static String getRenderer() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().deviceName : "n/a";
    }

    @Overwrite
    public static String getOpenGLVersion() {
        return Vulkan.getDevice() != null ? Vulkan.getDevice().driverVersion : "n/a";
    }

    @Overwrite
    public static String getCpuInfo() {
        return SystemInfo.cpuInfo;
    }
}
