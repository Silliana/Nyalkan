package net.nyalkan.mixin.compat.xaero;

import net.nyalkan.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.common.minimap.render.MinimapRenderer", remap = false)
public abstract class MinimapRendererM {

    @Redirect(method = "renderMinimap", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private void nyalkan$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }
}
