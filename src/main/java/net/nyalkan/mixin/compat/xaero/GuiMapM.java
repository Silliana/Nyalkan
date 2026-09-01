package net.nyalkan.mixin.compat.xaero;

import net.nyalkan.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.gui.GuiMap", remap = false)
public abstract class GuiMapM {

    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetError()I"))
    private int nyalkan$glGetError() {
        return 0;
    }

    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glClearColor(FFFF)V"))
    private void nyalkan$glClearColor(float red, float green, float blue, float alpha) {
    }

    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private void nyalkan$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    // Avoids ambiguous overload
    @Redirect(method = "render", require = 0, at = @At(value = "INVOKE", target = "Lxaero/map/exception/OpenGLException;checkGLError()V"))
    private void nyalkan$checkGLError() {
    }
}
