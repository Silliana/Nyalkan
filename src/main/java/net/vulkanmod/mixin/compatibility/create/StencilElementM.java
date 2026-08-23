package net.vulkanmod.mixin.compatibility.create;

import net.vulkanmod.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Redirects call site instead
@Mixin(targets = "net.createmod.catnip.gui.element.StencilElement", remap = false)
public interface StencilElementM {

    @Redirect(method = {"prepareStencil", "prepareElement", "cleanUp"},
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glEnable(I)V"))
    private void vulkanmod$glEnable(int target) {
        if (target == GL11.GL_STENCIL_TEST)
            VRenderSystem.enableStencilTest();
    }

    @Redirect(method = {"prepareStencil", "prepareElement", "cleanUp"},
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V"))
    private void vulkanmod$glDisable(int target) {
        if (target == GL11.GL_STENCIL_TEST)
            VRenderSystem.disableStencilTest();
    }
}
