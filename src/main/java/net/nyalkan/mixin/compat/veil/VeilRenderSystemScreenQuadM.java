package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// skips screen quad draw
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public class VeilRenderSystemScreenQuadM {

    @Inject(method = "drawScreenQuad", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nyalkan$drawScreenQuad(CallbackInfo ci) {
        ci.cancel();
    }

    @Redirect(method = "close", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL43C;glDeleteVertexArrays(I)V", remap = false))
    private static void nyalkan$glDeleteVertexArrays(int array) {
    }
}
