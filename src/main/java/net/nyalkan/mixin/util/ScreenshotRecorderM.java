package net.nyalkan.mixin.util;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Screenshot;
import net.nyalkan.gl.GlTexture;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.texture.VTextureSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(Screenshot.class)
public class ScreenshotRecorderM {

    @Overwrite
    public static NativeImage takeScreenshot(RenderTarget target) {
        int width = target.width;
        int height = target.height;

        NativeImage nativeimage = new NativeImage(width, height, false);
        GlTexture.bindTexture(target.getColorTextureId());
        // download reads unit zero
        VTextureSelector.bindTexture(0, GlTexture.getBoundTexture().getVulkanImage());

        // Need to submit and wait cmds if screenshot was requested
        // before the end of the frame
        Renderer.getInstance().flushCmds();

        nativeimage.downloadTexture(0, true);
        return nativeimage;
    }
}
