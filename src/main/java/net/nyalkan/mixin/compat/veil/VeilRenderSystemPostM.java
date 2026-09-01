package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// skips post draw
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public class VeilRenderSystemPostM {

    @Inject(method = "renderPost", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nyalkan$renderPost(CallbackInfo ci) {
        ci.cancel();
    }
}
