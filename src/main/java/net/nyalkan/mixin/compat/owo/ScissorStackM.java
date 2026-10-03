package net.nyalkan.mixin.compat.owo;

import com.mojang.blaze3d.platform.GlStateManager;
import net.nyalkan.vulkan.VRenderSystem;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw GL scissor calls
@Mixin(targets = "io.wispforest.owo.ui.util.ScissorStack", remap = false)
public abstract class ScissorStackM {

    @Redirect(method = "applyState", require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glScissor(IIII)V"))
    private static void nyalkan$glScissor(int x, int y, int width, int height) {
        GlStateManager._scissorBox(x, y, width, height);
    }

    @Redirect(method = {"applyState", "drawUnclipped"}, require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glIsEnabled(I)Z"))
    private static boolean nyalkan$glIsEnabled(int cap) {
        return cap == GL11.GL_SCISSOR_TEST && VRenderSystem.scissorTest;
    }

    @Redirect(method = "drawUnclipped", require = 0, at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_enableScissorTest()V"))
    private static void nyalkan$reenableScissor() {
        int[] box = VRenderSystem.scissorBox;
        GlStateManager._enableScissorTest();
        GlStateManager._scissorBox(box[0], box[1], box[2], box[3]);
    }
}
