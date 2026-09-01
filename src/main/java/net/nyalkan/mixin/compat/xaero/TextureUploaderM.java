package net.nyalkan.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.TextureUploader", remap = false)
public abstract class TextureUploaderM {

    @Redirect(method = "uploadTextures", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glFinish()V"))
    private void nyalkan$glFinish() {
    }
}
