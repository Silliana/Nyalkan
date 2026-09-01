package net.vulkanmod.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.graphics.TextureUploadBenchmark", remap = false)
public abstract class TextureUploadBenchmarkM {

    @Redirect(method = "post", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glFinish()V"))
    private void vulkanmod$glFinish() {
    }
}
