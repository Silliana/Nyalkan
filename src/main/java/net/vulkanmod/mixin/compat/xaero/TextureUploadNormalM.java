package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.TextureUpload$Normal", remap = false)
public abstract class TextureUploadNormalM {

    @Redirect(method = "upload", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glHint(II)V"))
    private void vulkanmod$glHint(int target, int mode) {
    }

    @Redirect(method = "upload", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexImage2D(IIIIIIIIJ)V"))
    private void vulkanmod$glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, long pixels) {
        GlTexture.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }
}
