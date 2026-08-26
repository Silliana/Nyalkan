package net.vulkanmod.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// stencil format query noop
@Mixin(targets = "foundry.veil.impl.client.render.wrapper.VanillaAdvancedFboWrapper", remap = false)
public class VanillaAdvancedFboWrapperM {

    @Redirect(method = "hasStencilAttachment", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL30C;glGetTexLevelParameteri(III)I", remap = false))
    private int vulkanmod$glGetTexLevelParameteri(int target, int level, int pname) {
        return 0;
    }
}
