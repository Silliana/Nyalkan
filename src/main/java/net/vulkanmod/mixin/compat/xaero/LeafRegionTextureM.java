package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.map.region.texture.LeafRegionTexture", remap = false)
public abstract class LeafRegionTextureM {

    @Redirect(method = "onDownloadedBuffer", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetTexLevelParameteri(III)I"))
    private int vulkanmod$glGetTexLevelParameteri(int target, int level, int pname) {
        return GlTexture.getTexLevelParameter(target, level, pname);
    }
}
