package net.vulkanmod.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// spec minimum limit
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem$2", remap = false)
public class VeilRenderSystemIntQueryM {

    @Inject(method = "getAsInt", at = @At("HEAD"), cancellable = true, remap = false)
    private void vulkanmod$getAsInt(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(4096);
    }
}
