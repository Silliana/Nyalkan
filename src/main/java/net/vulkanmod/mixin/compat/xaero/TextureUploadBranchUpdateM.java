package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.TextureUpload$BranchUpdate", remap = false)
public abstract class TextureUploadBranchUpdateM {

    @Redirect(method = "upload", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexImage2D(IIIIIIIILjava/nio/ByteBuffer;)V"))
    private void vulkanmod$glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        GlTexture.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Redirect(method = "upload", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetTexImage(IIIIJ)V"))
    private void vulkanmod$glGetTexImage(int tex, int level, int format, int type, long pixels) {
        GlTexture.getTexImage(tex, level, format, type, pixels);
    }
}
