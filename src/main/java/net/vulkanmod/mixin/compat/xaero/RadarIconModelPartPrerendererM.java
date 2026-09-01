package net.vulkanmod.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.hud.minimap.radar.icon.creator.render.form.model.part.RadarIconModelPartPrerenderer", remap = false)
public abstract class RadarIconModelPartPrerendererM {

    @Redirect(method = "renderPart", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetError()I"))
    private int vulkanmod$glGetError() {
        return 0;
    }
}
