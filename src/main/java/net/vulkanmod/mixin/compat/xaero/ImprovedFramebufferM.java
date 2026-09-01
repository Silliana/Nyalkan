package net.vulkanmod.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.common.graphics.ImprovedFramebuffer", remap = false)
public abstract class ImprovedFramebufferM {

    @Redirect(method = "detectOptifineFBOs", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I"))
    private static int vulkanmod$glGetInteger(int pname) {
        return 0;
    }
}
