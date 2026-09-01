package net.vulkanmod.mixin.compat.xaero;

import net.vulkanmod.gl.GlTexture;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "xaero.hud.minimap.radar.icon.creator.RadarIconCreator", remap = false)
public abstract class RadarIconCreatorM {

    @Redirect(method = "initFramebuffers", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameteri(III)V"))
    private void vulkanmod$glTexParameteri(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @Redirect(method = "initFramebuffers", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glTexParameterf(IIF)V"))
    private void vulkanmod$glTexParameterf(int target, int pname, float param) {
        GlTexture.texParameteri(target, pname, (int) param);
    }

    // vulkan depth range
    @Redirect(method = "setupMatrices", require = 0, at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;setOrtho(FFFFFF)Lorg/joml/Matrix4f;"))
    private Matrix4f vulkanmod$setOrtho(Matrix4f instance, float left, float right, float bottom, float top, float zNear, float zFar) {
        return instance.setOrtho(left, right, bottom, top, zNear, zFar, true);
    }
}
