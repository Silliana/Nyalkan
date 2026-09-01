package net.nyalkan.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// no gl driver query
@Mixin(targets = "xaero.map.MapLimiter", remap = false)
public abstract class MapLimiterM {

    @Unique private static final int UNKNOWN_DRIVER = 2;

    @Shadow private int driverType;

    @Inject(method = "determineDriverType", at = @At("HEAD"), cancellable = true)
    private void nyalkan$determineDriverType(CallbackInfo ci) {
        this.driverType = UNKNOWN_DRIVER;
        ci.cancel();
    }
}
