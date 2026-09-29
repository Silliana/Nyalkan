package net.nyalkan.mixin.compat.cyclops;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw gl depth test
@Mixin(targets = "org.cyclops.cyclopscore.helper.GuiHelpers", remap = false)
public abstract class GuiHelpersM {

    @Redirect(method = "drawTooltip", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"))
    private static void nyalkan$glDisable(int cap) {
        GlStateManager._disableDepthTest();
    }

    @Redirect(method = "drawTooltip", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
    private static void nyalkan$glEnable(int cap) {
        GlStateManager._enableDepthTest();
    }
}
