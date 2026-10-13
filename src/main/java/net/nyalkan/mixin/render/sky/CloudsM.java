package net.nyalkan.mixin.render.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.nyalkan.render.profiling.Profiler;
import net.nyalkan.render.sky.CloudRenderer;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class CloudsM {
    @Shadow private int ticks;
    @Shadow private @Nullable ClientLevel level;

    @Unique private CloudRenderer nyalkan$clouds;

    // after dimension cloud hook
    @Inject(method = "renderClouds", cancellable = true, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getCloudHeight()F"))
    private void nyalkan$renderClouds(PoseStack poseStack, Matrix4f modelView, Matrix4f projection, float partialTick,
                                        double camX, double camY, double camZ, CallbackInfo ci) {
        ci.cancel();
        float height = this.level.effects().getCloudHeight();
        if (Float.isNaN(height))
            return;

        if (this.nyalkan$clouds == null)
            this.nyalkan$clouds = new CloudRenderer();

        Profiler profiler = Profiler.getMainProfiler();
        profiler.push("Clouds");
        this.nyalkan$clouds.renderClouds(this.level, height, poseStack, modelView, projection, this.ticks, partialTick, camX, camY, camZ);
        profiler.pop();
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void nyalkan$resetClouds(CallbackInfo ci) {
        if (this.nyalkan$clouds != null)
            this.nyalkan$clouds.resetBuffer();
    }

    @Inject(method = "onResourceManagerReload", at = @At("RETURN"))
    private void nyalkan$reloadClouds(ResourceManager resourceManager, CallbackInfo ci) {
        if (this.nyalkan$clouds != null)
            this.nyalkan$clouds.loadTexture();
    }
}
