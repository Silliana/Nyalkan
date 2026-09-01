package net.vulkanmod.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "xaero.map.exception.OpenGLException", remap = false)
public abstract class OpenGLExceptionWorldMapM {

    @Inject(method = "checkGLError(ZLjava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private static void vulkanmod$checkGLError(boolean condition, String message, CallbackInfo ci) {
        ci.cancel();
    }
}
