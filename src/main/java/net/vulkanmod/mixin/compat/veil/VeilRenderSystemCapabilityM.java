package net.vulkanmod.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// all gl checks unsupported
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem$1", remap = false)
public class VeilRenderSystemCapabilityM {

    @Inject(method = "getAsBoolean", at = @At("HEAD"), cancellable = true, remap = false)
    private void vulkanmod$getAsBoolean(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
