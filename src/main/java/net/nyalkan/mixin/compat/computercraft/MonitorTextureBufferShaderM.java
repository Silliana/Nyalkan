package net.nyalkan.mixin.compat.computercraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// fail TBO shader cleanly
@Mixin(targets = "dan200.computercraft.client.render.monitor.MonitorTextureBufferShader", remap = false)
public abstract class MonitorTextureBufferShaderM {

    @Redirect(method = "<init>", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL31;glGetUniformBlockIndex(ILjava/lang/CharSequence;)I"))
    private int nyalkan$glGetUniformBlockIndex(int program, CharSequence name) {
        throw new UnsupportedOperationException("Texture buffer monitor shader requires OpenGL");
    }
}
