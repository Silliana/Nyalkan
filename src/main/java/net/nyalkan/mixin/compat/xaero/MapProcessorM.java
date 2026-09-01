package net.nyalkan.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.MapProcessor", remap = false)
public abstract class MapProcessorM {

    @Redirect(method = "onRenderProcess", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetError()I"))
    private int nyalkan$glGetError() {
        return 0;
    }

    @Redirect(method = "onRenderProcess", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClearColor(FFFF)V"))
    private void nyalkan$glClearColor(float red, float green, float blue, float alpha) {
    }

    // Avoids ambiguous overload
    @Redirect(method = "onRenderProcess", require = 0, at = @At(value = "INVOKE", target = "Lxaero/map/exception/OpenGLException;checkGLError()V"))
    private void nyalkan$checkGLError() {
    }
}
