package net.nyalkan.mixin.compat.xaero;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.misc.Misc", remap = false)
public abstract class MiscM {

    @Redirect(method = {"glTimerPre", "glTimerResult"}, require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glFinish()V"))
    private static void nyalkan$glFinish() {
    }

    // vulkan depth range
    @Redirect(method = "minecraftOrtho", require = 0, at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;setOrtho(FFFFFF)Lorg/joml/Matrix4f;"))
    private static Matrix4f nyalkan$setOrtho(Matrix4f instance, float left, float right, float bottom, float top, float zNear, float zFar) {
        return instance.setOrtho(left, right, bottom, top, zNear, zFar, true);
    }
}
