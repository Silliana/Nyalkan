package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.icon.XaeroIconAtlas$Builder", remap = false)
public abstract class XaeroIconAtlasWorldMapBuilderM {

    @Redirect(method = "createGlTexture", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private void vulkanmod$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @Redirect(method = "createGlTexture", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameterf(IIF)V"))
    private void vulkanmod$glTexParameterf(int target, int pname, float param) {
        GlTexture.texParameteri(target, pname, (int) param);
    }

    // Avoids ambiguous overload
    @Redirect(method = "createGlTexture", require = 0, at = @At(value = "INVOKE", target = "Lxaero/map/exception/OpenGLException;checkGLError()V"))
    private void vulkanmod$checkGLError() {
    }
}
