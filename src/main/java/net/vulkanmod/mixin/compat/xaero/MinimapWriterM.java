package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.common.minimap.write.MinimapWriter", remap = false)
public abstract class MinimapWriterM {

    @Redirect(method = "writeChunk", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures(I)V"))
    private void vulkanmod$glDeleteTextures(int texture) {
        GlTexture.glDeleteTextures(texture);
    }
}
