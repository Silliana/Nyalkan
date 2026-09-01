package net.nyalkan.mixin.compat.xaero;

import net.nyalkan.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.TextureUpload$BranchDownload", remap = false)
public abstract class TextureUploadBranchDownloadM {

    @Redirect(method = "upload", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetTexImage(IIIIJ)V"))
    private void nyalkan$glGetTexImage(int tex, int level, int format, int type, long pixels) {
        GlTexture.getTexImage(tex, level, format, type, pixels);
    }
}
