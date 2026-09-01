package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlBuffer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

// Redirects call site instead
@Mixin(targets = "xaero.common.graphics.PixelBuffers", remap = false)
public abstract class PixelBuffersM {

    @Redirect(method = "glGenBuffers", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glGenBuffers()I"))
    private static int vulkanmod$glGenBuffers() {
        return GlBuffer.glGenBuffers();
    }

    @Redirect(method = "glBindBuffer", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glBindBuffer(II)V"))
    private static void vulkanmod$glBindBuffer(int target, int buffer) {
        GlBuffer.glBindBuffer(target, buffer);
    }

    @Redirect(method = "glBufferData", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glBufferData(IJI)V"))
    private static void vulkanmod$glBufferData(int target, long size, int usage) {
        GlBuffer.glBufferData(target, size, usage);
    }

    @Redirect(method = "glMapBuffer(IIJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glMapBuffer(IIJLjava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;"))
    private static ByteBuffer vulkanmod$glMapBuffer4(int target, int access, long length, @Nullable ByteBuffer old_buffer) {
        return GlBuffer.glMapBuffer(target, access);
    }

    @Redirect(method = "glMapBuffer(II)Ljava/nio/ByteBuffer;", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glMapBuffer(II)Ljava/nio/ByteBuffer;"))
    private static ByteBuffer vulkanmod$glMapBuffer2(int target, int access) {
        return GlBuffer.glMapBuffer(target, access);
    }

    @Redirect(method = "glUnmapBuffer", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glUnmapBuffer(I)Z"))
    private static boolean vulkanmod$glUnmapBuffer(int target) {
        return GlBuffer.glUnmapBuffer(target);
    }

    @Redirect(method = "glDeleteBuffers(I)V", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glDeleteBuffers(I)V"))
    private static void vulkanmod$glDeleteBuffers(int buffer) {
        GlBuffer.glDeleteBuffers(buffer);
    }

    @Redirect(method = "glDeleteBuffers(Ljava/nio/IntBuffer;)V", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15;glDeleteBuffers(Ljava/nio/IntBuffer;)V"))
    private static void vulkanmod$glDeleteBuffersBuf(IntBuffer buffers) {
        GlBuffer.glDeleteBuffers(buffers);
    }
}
