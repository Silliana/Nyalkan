package net.vulkanmod.mixin.compat.ftb;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw gl scissor box
@Mixin(targets = "dev.ftb.mods.ftblibrary.ui.GuiHelper$Scissor", remap = false)
public abstract class GuiHelperScissorM {

    @Redirect(method = "scissor", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScissor(IIII)V"))
    private void vulkanmod$glScissor(int x, int y, int width, int height) {
        GlStateManager._scissorBox(x, y, width, height);
    }
}
