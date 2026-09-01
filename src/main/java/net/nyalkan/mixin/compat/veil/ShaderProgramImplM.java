package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// skips gl compile link
@Mixin(targets = "foundry.veil.impl.client.render.shader.program.ShaderProgramImpl", remap = false)
public class ShaderProgramImplM {

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true, remap = false)
    private void nyalkan$compile(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "recompile", at = @At("HEAD"), cancellable = true, remap = false)
    private void nyalkan$recompile(CallbackInfo ci) {
        ci.cancel();
    }
}
