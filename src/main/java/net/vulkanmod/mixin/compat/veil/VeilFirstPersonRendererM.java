package net.vulkanmod.mixin.compat.veil;

import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Restores real screen clear
@Mixin(targets = "foundry.veil.impl.client.render.pipeline.VeilFirstPersonRenderer", remap = false)
public class VeilFirstPersonRendererM {

    @Inject(method = "bind", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$bind(int mask, CallbackInfo ci) {
        RenderSystem.clear(mask, false);
        ci.cancel();
    }

    @Inject(method = "unbind", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$unbind(CallbackInfo ci) {
        ci.cancel();
    }
}
