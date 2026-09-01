package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.common.minimap.render.MinimapFBORenderer", remap = false)
public abstract class MinimapFBORendererM {

    @Redirect(method = "renderChunksToFBO", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClear(I)V"))
    private void vulkanmod$glClear(int mask) {
        VRenderSystem.clear(mask);
    }

    @Redirect(method = "renderChunksToFBO", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private void vulkanmod$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @Redirect(method = "renderChunksToFBO", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glBindTexture(II)V"))
    private void vulkanmod$glBindTexture(int target, int texture) {
        GlTexture.bindTexture(texture);
    }
}
