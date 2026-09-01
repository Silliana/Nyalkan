package net.nyalkan.mixin.compat.xaero;

import net.nyalkan.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

// Redirects call site instead
@Mixin(targets = "xaero.common.minimap.region.MinimapChunk", remap = false)
public abstract class MinimapChunkM {

    @Redirect(method = "bindTexture", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGenTextures()I"))
    private int nyalkan$glGenTextures() {
        return GlTexture.genTextureId();
    }

    @Redirect(method = "bindTexture", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private void nyalkan$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @Redirect(method = "bindTexture", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameterf(IIF)V"))
    private void nyalkan$glTexParameterf(int target, int pname, float param) {
        GlTexture.texParameteri(target, pname, (int) param);
    }

    @Redirect(method = "bindTexture", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexImage2D(IIIIIIIILjava/nio/ByteBuffer;)V"))
    private void nyalkan$glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {
        GlTexture.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Redirect(method = "bindTexture", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexSubImage2D(IIIIIIIIJ)V"))
    private void nyalkan$glTexSubImage2D(int target, int level, int xOffset, int yOffset, int width, int height, int format, int type, long pixels) {
        GlTexture.texSubImage2D(target, level, xOffset, yOffset, width, height, format, type, pixels);
    }

    @Redirect(method = "cleanup", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures(I)V"))
    private void nyalkan$glDeleteTextures(int texture) {
        GlTexture.glDeleteTextures(texture);
    }
}
