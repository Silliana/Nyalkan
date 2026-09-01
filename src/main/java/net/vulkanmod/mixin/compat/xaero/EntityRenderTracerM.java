package net.vulkanmod.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.hud.minimap.radar.icon.creator.render.trace.EntityRenderTracer", remap = false)
public abstract class EntityRenderTracerM {

    @Redirect(method = "trace", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetError()I"))
    private int vulkanmod$glGetError() {
        return 0;
    }

    @Redirect(method = "fixAdditiveTransparency", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I"))
    private int vulkanmod$glGetInteger(int pname) {
        return 0;
    }
}
