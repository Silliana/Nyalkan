package net.nyalkan.mixin.compat.xaero;

import net.nyalkan.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.GLObjectDeleter", remap = false)
public abstract class GLObjectDeleterM {

    @Redirect(method = "work", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures(Ljava/nio/IntBuffer;)V"))
    private void nyalkan$glDeleteTextures(IntBuffer textures) {
        GlTexture.glDeleteTextures(textures);
    }
}
