package net.nyalkan.mixin.compat.veil;

import net.nyalkan.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// force disabled veil debug
@Mixin(targets = "foundry.veil.api.client.render.ext.VeilDebug", remap = false)
public class VeilDebugMixin {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nyalkan$get(CallbackInfoReturnable<Object> cir) {
        try {
            Class<?> veilDebugClass = Class.forName("foundry.veil.api.client.render.ext.VeilDebug");
            cir.setReturnValue(Enum.valueOf((Class<Enum>) (Class) veilDebugClass, "DISABLED"));
        } catch (ReflectiveOperationException e) {
            Initializer.LOGGER.error("[Nyalkan] Failed to disable Veil debug", e);
        }
    }
}
