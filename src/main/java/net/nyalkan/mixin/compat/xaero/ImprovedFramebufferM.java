package net.nyalkan.mixin.compat.xaero;

import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.common.graphics.ImprovedFramebuffer", remap = false)
public abstract class ImprovedFramebufferM {

    @Redirect(method = "detectOptifineFBOs", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I"))
    private static int nyalkan$glGetInteger(int pname) {
        return 0;
    }

    @Redirect(method = "generateMipmaps", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glGenerateMipmap(I)V"))
    private void nyalkan$glGenerateMipmap(int target) {
    }

    // offscreen keeps gl orientation
    @Redirect(method = "bindWrite", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_viewport(IIII)V"))
    private void nyalkan$viewport(int x, int y, int width, int height) {
        ImmediateBatcher.INSTANCE.flushGui();
        Renderer.setInvertedViewport(x, y, width, height);
    }
}
