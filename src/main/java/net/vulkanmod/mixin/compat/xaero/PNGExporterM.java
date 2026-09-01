package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;

// Redirects call site instead
@Mixin(targets = "xaero.map.file.export.PNGExporter", remap = false)
public abstract class PNGExporterM {

    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I"))
    private int vulkanmod$glGetInteger(int pname) {
        return 0;
    }

    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL13;glCompressedTexImage2D(IIIIIILjava/nio/ByteBuffer;)V"))
    private void vulkanmod$glCompressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, ByteBuffer data) {
    }

    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexImage2D(IIIIIIIILjava/nio/ByteBuffer;)V"))
    private void vulkanmod$glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, @Nullable ByteBuffer pixels) {
        GlTexture.texImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }

    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private void vulkanmod$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDeleteTextures([I)V"))
    private void vulkanmod$glDeleteTextures(int[] textures) {
        for (int texture : textures)
            GlTexture.glDeleteTextures(texture);
    }

    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetTexImage(IIIILjava/nio/ByteBuffer;)V"))
    private void vulkanmod$glGetTexImage(int tex, int level, int format, int type, ByteBuffer pixels) {
        GlTexture.getTexImage(tex, level, format, type, MemoryUtil.memAddress(pixels));
    }

    // Avoids ambiguous overload
    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lxaero/map/exception/OpenGLException;checkGLError()V"))
    private void vulkanmod$checkGLError() {
    }

    // vulkan depth range
    @Redirect(method = "export", require = 0, at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;setOrtho(FFFFFF)Lorg/joml/Matrix4f;"))
    private Matrix4f vulkanmod$setOrtho(Matrix4f instance, float left, float right, float bottom, float top, float zNear, float zFar) {
        return instance.setOrtho(left, right, bottom, top, zNear, zFar, true);
    }
}
