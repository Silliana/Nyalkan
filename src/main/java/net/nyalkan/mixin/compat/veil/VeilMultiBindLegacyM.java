package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// legacy multi bind noop
@Mixin(targets = "foundry.veil.api.client.render.ext.VeilMultiBind$1", remap = false)
public class VeilMultiBindLegacyM {

    @Redirect(method = "bindTextures", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11C;glBindTexture(II)V", remap = false))
    private void nyalkan$glBindTexture(int target, int texture) {
    }

    @Redirect(method = "bindSamplers", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glBindSampler(II)V", remap = false))
    private void nyalkan$glBindSampler(int unit, int sampler) {
    }
}
