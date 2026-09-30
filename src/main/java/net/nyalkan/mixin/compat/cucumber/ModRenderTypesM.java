package net.nyalkan.mixin.compat.cucumber;

import net.nyalkan.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw gl blend color
@Mixin(targets = "com.blakebr0.cucumber.client.ModRenderTypes", remap = false)
public abstract class ModRenderTypesM {

    @Redirect(method = {"lambda$static$0", "lambda$static$1"}, require = 0, at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL14;glBlendColor(FFFF)V"))
    private static void nyalkan$glBlendColor(float red, float green, float blue, float alpha) {
        VRenderSystem.blendColor(red, green, blue, alpha);
    }
}
