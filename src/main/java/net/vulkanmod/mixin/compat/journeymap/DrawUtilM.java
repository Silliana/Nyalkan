package net.vulkanmod.mixin.compat.journeymap;

import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// vulkan depth range
@Mixin(targets = "journeymap.client.render.draw.DrawUtil", remap = false)
public abstract class DrawUtilM {

    @Redirect(method = "sizeDisplay", require = 0, at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;setOrtho(FFFFFF)Lorg/joml/Matrix4f;"))
    private static Matrix4f vulkanmod$setOrtho(Matrix4f instance, float left, float right, float bottom, float top, float zNear, float zFar) {
        return instance.setOrtho(left, right, bottom, top, zNear, zFar, true);
    }
}
