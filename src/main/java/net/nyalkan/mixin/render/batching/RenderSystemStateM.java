package net.nyalkan.mixin.render.batching;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.resources.ResourceLocation;
import net.nyalkan.render.compat.SkyTargetGate;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// flush before state changes
@Mixin(value = RenderSystem.class, remap = false)
public class RenderSystemStateM {

    @Inject(method = "setShaderFogStart", at = @At("HEAD"))
    private static void nyalkan$fogStart(float start, CallbackInfo ci) {
        if (RenderSystem.getShaderFogStart() != start)
            ImmediateBatcher.INSTANCE.beforeStateChange();
    }

    @Inject(method = "setShaderFogEnd", at = @At("HEAD"))
    private static void nyalkan$fogEnd(float end, CallbackInfo ci) {
        if (RenderSystem.getShaderFogEnd() != end)
            ImmediateBatcher.INSTANCE.beforeStateChange();
    }

    @Inject(method = "setShaderFogShape", at = @At("HEAD"))
    private static void nyalkan$fogShape(FogShape shape, CallbackInfo ci) {
        if (RenderSystem.getShaderFogShape() != shape)
            ImmediateBatcher.INSTANCE.beforeStateChange();
    }

    @Inject(method = "_setShaderTexture(II)V", at = @At("HEAD"))
    private static void nyalkan$texture(int unit, int id, CallbackInfo ci) {
        SkyTargetGate.noteTexture(id);
        if (unit >= 0 && unit < 12 && RenderSystem.getShaderTexture(unit) != id)
            ImmediateBatcher.INSTANCE.textureChanged();
    }

    @Inject(method = "_setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", at = @At("HEAD"))
    private static void nyalkan$textureLocation(int unit, ResourceLocation location, CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.textureChanged();
    }

    @Inject(method = "setShaderGameTime", at = @At("HEAD"))
    private static void nyalkan$gameTime(long tickTime, float partialTicks, CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.beforeStateChange();
    }
}
