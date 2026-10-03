package net.nyalkan.mixin.compat.owo;

import net.nyalkan.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw GL scissor query
@Mixin(targets = "io.wispforest.owo.ui.base.BaseOwoHandledScreen$SlotComponent", remap = false)
public abstract class SlotComponentM {

    @Redirect(method = "draw", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glGetIntegerv(I[I)V"))
    private void nyalkan$glGetIntegerv(int pname, int[] params) {
        if (pname == GL11.GL_SCISSOR_BOX)
            System.arraycopy(VRenderSystem.scissorBox, 0, params, 0, Math.min(4, params.length));
    }
}
