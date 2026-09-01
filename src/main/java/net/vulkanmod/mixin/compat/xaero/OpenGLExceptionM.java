package net.vulkanmod.mixin.compat.xaero;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.common.exception.OpenGLException", remap = false)
public abstract class OpenGLExceptionM {

    @Redirect(method = "checkGLError", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetError()I"))
    private static int vulkanmod$glGetError() {
        return 0;
    }
}
