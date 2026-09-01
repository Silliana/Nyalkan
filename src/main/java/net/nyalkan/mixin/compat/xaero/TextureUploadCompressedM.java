package net.nyalkan.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.TextureUpload$Compressed", remap = false)
public abstract class TextureUploadCompressedM {

    @Redirect(method = "upload", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL13;glCompressedTexImage2D(IIIIIIIJ)V"))
    private void nyalkan$glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, long data) {
    }
}
