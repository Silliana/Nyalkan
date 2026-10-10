package net.nyalkan.mixin.compat.accessories;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw GL framebuffer blit
@Mixin(targets = "io.wispforest.accessories.client.PostEffectBuffer", remap = false)
public abstract class PostEffectBufferM {

    @Redirect(method = "beginWrite", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30;glBlitFramebuffer(IIIIIIIIII)V"))
    private void nyalkan$glBlitFramebuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        GlStateManager._glBlitFrameBuffer(srcX0, srcY0, srcX1, srcY1, dstX0, dstY0, dstX1, dstY1, mask, filter);
    }
}
