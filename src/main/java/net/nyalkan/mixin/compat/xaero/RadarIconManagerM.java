package net.nyalkan.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.hud.minimap.radar.icon.RadarIconManager", remap = false)
public abstract class RadarIconManagerM {

    @Redirect(method = "get", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetError()I"))
    private int nyalkan$glGetError() {
        return 0;
    }
}
