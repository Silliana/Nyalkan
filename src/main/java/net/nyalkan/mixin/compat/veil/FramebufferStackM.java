package net.nyalkan.mixin.compat.veil;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.nyalkan.gl.GlFramebufferStackState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.IntBuffer;

// real framebuffer state queries
@Mixin(targets = "foundry.veil.api.client.render.framebuffer.FramebufferStack", remap = false)
public class FramebufferStackM {

    // Returns tracked real viewport
    @Redirect(method = "push", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glGetIntegerv(ILjava/nio/IntBuffer;)V", remap = false))
    private static void nyalkan$glGetIntegerv(int pname, IntBuffer params) {
        if (pname == 0x0BA2 && params.remaining() >= 4) {
            int pos = params.position();
            params.put(pos, GlFramebufferStackState.vx).put(pos + 1, GlFramebufferStackState.vy)
                    .put(pos + 2, GlFramebufferStackState.vw).put(pos + 3, GlFramebufferStackState.vh);
            return;
        }
        for (int i = 0; i < params.remaining(); i++)
            params.put(params.position() + i, 0);
    }

    @Redirect(method = "push", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glGetInteger(I)I", remap = false))
    private static int nyalkan$glGetInteger(int pname) {
        if (pname == 0x8CA6 || pname == 0x8CAA)
            return GlFramebufferStackState.boundFbo;
        return 0;
    }

    // Records real viewport calls
    @Mixin(RenderSystem.class)
    public static class ViewportTrackerM {
        @Inject(method = "viewport", at = @At("TAIL"))
        private static void nyalkan$trackViewport(int x, int y, int width, int height, CallbackInfo ci) {
            GlFramebufferStackState.vx = x;
            GlFramebufferStackState.vy = y;
            GlFramebufferStackState.vw = width;
            GlFramebufferStackState.vh = height;
        }
    }

    // Records real framebuffer binds
    @Mixin(GlStateManager.class)
    public static class FramebufferTrackerM {
        @Inject(method = "_glBindFramebuffer", at = @At("TAIL"), remap = false)
        private static void nyalkan$trackFramebuffer(int target, int fbo, CallbackInfo ci) {
            GlFramebufferStackState.boundFbo = fbo;
        }
    }
}
