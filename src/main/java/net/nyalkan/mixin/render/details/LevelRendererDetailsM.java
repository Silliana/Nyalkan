package net.nyalkan.mixin.render.details;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.nyalkan.Initializer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererDetailsM {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void nyalkan$sky(Matrix4f frustumMatrix, Matrix4f projectionMatrix, float partialTick, Camera camera,
                             boolean isFoggy, Runnable skyFogSetup, CallbackInfo ci) {
        if (!Initializer.CONFIG.renderSky)
            ci.cancel();
    }

    // sun quad half size
    @ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 30.0F))
    private float nyalkan$sun(float size) {
        return Initializer.CONFIG.renderSun ? size : 0.0F;
    }

    // moon quad half size
    @ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 20.0F))
    private float nyalkan$moon(float size) {
        return Initializer.CONFIG.renderMoon ? size : 0.0F;
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"))
    private float nyalkan$stars(ClientLevel level, float partialTick) {
        return Initializer.CONFIG.renderStars ? level.getStarBrightness(partialTick) : 0.0F;
    }

    @Inject(method = "renderSnowAndRain", at = @At("HEAD"), cancellable = true)
    private void nyalkan$weather(LightTexture lightTexture, float partialTick, double camX, double camY, double camZ, CallbackInfo ci) {
        if (!Initializer.CONFIG.renderWeather)
            ci.cancel();
    }

    // splashes and rain sounds
    @Inject(method = "tickRain", at = @At("HEAD"), cancellable = true)
    private void nyalkan$weatherTick(Camera camera, CallbackInfo ci) {
        if (!Initializer.CONFIG.renderWeather)
            ci.cancel();
    }
}
