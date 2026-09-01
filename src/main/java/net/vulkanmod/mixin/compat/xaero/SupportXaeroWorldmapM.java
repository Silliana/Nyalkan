package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.common.mods.SupportXaeroWorldmap", remap = false)
public abstract class SupportXaeroWorldmapM {

    @Redirect(method = "renderChunks", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameterf(IIF)V"))
    private void vulkanmod$glTexParameterf(int target, int pname, float param) {
        GlTexture.texParameteri(target, pname, (int) param);
    }

    @Redirect(method = {"lambda$drawMinimap$0", "lambda$drawMinimap$1", "lambda$drawMinimap$2"}, require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private static void vulkanmod$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }
}
