package net.nyalkan.mixin.compat.fancymenu;

import net.nyalkan.gl.GlTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw GL texture queries
@Mixin(targets = "de.keksuccino.fancymenu.util.rendering.GuiTextureSamplerUtil$OpenGlTextureParameterBackend", remap = false)
public abstract class GuiTextureSamplerUtilM {

    @Redirect(method = "getBoundTexture2D", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetInteger(I)I"))
    private int nyalkan$glGetInteger(int pname) {
        return GlTexture.getBoundTextureId();
    }

    @Redirect(method = "getTextureParameter", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetTexParameteri(II)I"))
    private int nyalkan$glGetTexParameteri(int target, int pname) {
        return GlTexture.getTexParameter(target, pname);
    }
}
