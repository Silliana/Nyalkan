package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.TextureUpload$SubsequentNormal", remap = false)
public abstract class TextureUploadSubsequentNormalM {

    @Redirect(method = "upload", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexSubImage2D(IIIIIIIIJ)V"))
    private void vulkanmod$glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, long pixels) {
        GlTexture.texSubImage2D(target, level, xOffset, yOffset, width, height, format, type, pixels);
    }
}
