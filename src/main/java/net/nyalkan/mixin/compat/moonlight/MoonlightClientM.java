package net.nyalkan.mixin.compat.moonlight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// no raw gl mipmaps
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.moonlight.core.MoonlightClient", remap = false)
public abstract class MoonlightClientM {

    @Inject(method = "isMapMipMap", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nyalkan$noMapMipMap(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(false);
    }
}
