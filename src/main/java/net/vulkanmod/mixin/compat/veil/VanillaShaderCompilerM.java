package net.vulkanmod.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

// skips parallel precompile
@Mixin(targets = "foundry.veil.impl.client.render.dynamicbuffer.VanillaShaderCompiler", remap = false)
public class VanillaShaderCompilerM {

    @Inject(method = "reload", at = @At("HEAD"), cancellable = true, remap = false)
    private void vulkanmod$reload(CallbackInfoReturnable<CompletableFuture<?>> cir) {
        cir.setReturnValue(CompletableFuture.completedFuture(null));
    }
}
