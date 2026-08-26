package net.vulkanmod.mixin.compat.veil;

import net.vulkanmod.Initializer;
import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

// raw vao calls neutralised
@Mixin(targets = "foundry.veil.api.client.render.vertex.VertexArray", remap = false)
public class VertexArrayM {

    @Inject(method = "loadType", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$loadType(CallbackInfo ci) {
        try {
            Class<?> vertexArrayClass = Class.forName("foundry.veil.api.client.render.vertex.VertexArray");
            Field typeField = vertexArrayClass.getDeclaredField("vertexArrayType");
            typeField.setAccessible(true);
            if (typeField.get(null) == null) {
                Class<?> typeEnumClass = Class.forName("foundry.veil.api.client.render.vertex.VertexArray$VertexArrayType");
                Object legacy = Enum.valueOf((Class<Enum>) (Class) typeEnumClass, "LEGACY");
                typeField.set(null, legacy);
            }
        } catch (ReflectiveOperationException e) {
            Initializer.LOGGER.error("[VulkanMod] Failed to force VertexArray$VertexArrayType.LEGACY", e);
        }
        ci.cancel();
    }

    @Redirect(method = "create", remap = false,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glGenVertexArrays()I", remap = false))
    private static int vulkanmod$glGenVertexArrays() {
        return GlTexture.genTextureId();
    }

    @Redirect(method = "create", remap = false,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glCreateVertexArrays()I", remap = false))
    private static int vulkanmod$glCreateVertexArrays() {
        return GlTexture.genTextureId();
    }

    @Redirect(method = "create([Lfoundry/veil/api/client/render/vertex/VertexArray;)V", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glGenVertexArrays(Ljava/nio/IntBuffer;)V", remap = false))
    private static void vulkanmod$glGenVertexArraysBatch(IntBuffer arrays) {
        for (int i = 0; i < arrays.remaining(); i++)
            arrays.put(arrays.position() + i, GlTexture.genTextureId());
    }

    @Redirect(method = "create([Lfoundry/veil/api/client/render/vertex/VertexArray;)V", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glCreateVertexArrays(Ljava/nio/IntBuffer;)V", remap = false))
    private static void vulkanmod$glCreateVertexArraysBatch(IntBuffer arrays) {
        for (int i = 0; i < arrays.remaining(); i++)
            arrays.put(arrays.position() + i, GlTexture.genTextureId());
    }

    private static final String UPLOAD = "upload(ILjava/nio/ByteBuffer;Lfoundry/veil/api/client/render/vertex/VertexArray$DrawUsage;)V";

    @Redirect(method = UPLOAD, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glBindBuffer(II)V", remap = false))
    private static void vulkanmod$glBindBuffer(int target, int buffer) {
    }

    @Redirect(method = UPLOAD, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glBufferData(ILjava/nio/ByteBuffer;I)V", remap = false))
    private static void vulkanmod$glBufferData(int target, ByteBuffer data, int usage) {
    }

    // unused dsa path
    @Redirect(method = UPLOAD, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glNamedBufferData(ILjava/nio/ByteBuffer;I)V", remap = false))
    private static void vulkanmod$glNamedBufferData(int buffer, ByteBuffer data, int usage) {
    }

    @Redirect(method = {"draw()V", "drawInstanced(I)V"}, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glGetInteger(I)I", remap = false))
    private static int vulkanmod$glGetInteger(int pname) {
        return 0;
    }

    @Redirect(method = "draw()V", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glDrawArrays(III)V", remap = false))
    private static void vulkanmod$glDrawArrays(int mode, int first, int count) {
    }

    @Redirect(method = "draw()V", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glDrawElements(IIIJ)V", remap = false))
    private static void vulkanmod$glDrawElements(int mode, int count, int type, long indices) {
    }

    @Redirect(method = "drawInstanced(I)V", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31C;glDrawArraysInstanced(IIII)V", remap = false))
    private static void vulkanmod$glDrawArraysInstanced(int mode, int first, int count, int instanceCount) {
    }

    @Redirect(method = "drawInstanced(I)V", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31C;glDrawElementsInstanced(IIIJI)V", remap = false))
    private static void vulkanmod$glDrawElementsInstanced(int mode, int count, int type, long indices, int instanceCount) {
    }

    @Redirect(method = "drawIndirect(JII)V", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBMultiDrawIndirect;glMultiDrawElementsIndirect(IIJII)V", remap = false))
    private static void vulkanmod$glMultiDrawElementsIndirect(int mode, int type, long indirect, int drawCount, int stride) {
    }

    @Redirect(method = "free", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glDeleteBuffers([I)V", remap = false))
    private void vulkanmod$glDeleteBuffers(int[] buffers) {
    }

    @Redirect(method = "free", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glDeleteVertexArrays(I)V", remap = false))
    private void vulkanmod$glDeleteVertexArrays(int array) {
    }
}
