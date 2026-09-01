package net.nyalkan.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.hud.render.HudRenderer", remap = false)
public abstract class HudRendererM {

    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetError()I"))
    private int nyalkan$glGetError() {
        return 0;
    }

    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClearColor(FFFF)V"))
    private void nyalkan$glClearColor(float red, float green, float blue, float alpha) {
    }
}
