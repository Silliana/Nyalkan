package net.vulkanmod.mixin.compat.veil;

import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

// Neutralizes raw texture calls
@Mixin(targets = "foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManager", remap = false)
public class DynamicBufferManagerM {

    @Redirect(method = "<init>", remap = false,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL12C;glGenTextures(Ljava/nio/IntBuffer;)V", remap = false))
    private void vulkanmod$glGenTextures(IntBuffer textures) {
        for (int i = 0; i < textures.remaining(); i++) {
            textures.put(textures.position() + i, GlTexture.genTextureId());
        }
    }

    @Redirect(method = "free", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL12C;glDeleteTextures(Ljava/nio/IntBuffer;)V", remap = false))
    private void vulkanmod$glDeleteTextures(IntBuffer textures) {
    }

    // unhandled hdr format noop
    @Mixin(targets = "foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManager$DynamicBuffer", remap = false)
    public static class DynamicBufferM {
        @Redirect(method = {"init", "resize"}, remap = false,
                at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texImage2D(IIIIIIIILjava/nio/IntBuffer;)V", remap = false))
        private static void vulkanmod$texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, IntBuffer pixels) {
        }
    }
}
