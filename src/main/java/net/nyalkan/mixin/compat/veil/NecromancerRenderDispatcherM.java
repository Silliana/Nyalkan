package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.IntBuffer;

// raw skeleton buffer noop
@Mixin(targets = "foundry.veil.impl.client.necromancer.render.NecromancerRenderDispatcher", remap = false)
public class NecromancerRenderDispatcherM {

    @Redirect(method = "delete", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glDeleteBuffers(Ljava/nio/IntBuffer;)V", remap = false))
    private static void nyalkan$glDeleteBuffers(IntBuffer buffers) {
    }

    @Redirect(method = "updateBlockSize", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glNamedBufferData(IJI)V", remap = false))
    private static void nyalkan$glNamedBufferData(int buffer, long size, int usage) {
    }

    @Redirect(method = "updateBlockSize", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glBindBuffer(II)V", remap = false))
    private static void nyalkan$glBindBuffer(int target, int buffer) {
    }

    @Redirect(method = "updateBlockSize", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL15C;glBufferData(IJI)V", remap = false))
    private static void nyalkan$glBufferData(int target, long size, int usage) {
    }
}
