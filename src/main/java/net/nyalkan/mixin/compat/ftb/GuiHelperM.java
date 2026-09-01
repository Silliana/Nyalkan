package net.nyalkan.mixin.compat.ftb;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw gl scissor toggle
@Mixin(targets = "dev.ftb.mods.ftblibrary.ui.GuiHelper", remap = false)
public abstract class GuiHelperM {

    @Redirect(method = "pushScissor", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
    private static void nyalkan$glEnable(int cap) {
        GlStateManager._enableScissorTest();
    }

    @Redirect(method = "popScissor", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"))
    private static void nyalkan$glDisable(int cap) {
        GlStateManager._disableScissorTest();
    }
}
