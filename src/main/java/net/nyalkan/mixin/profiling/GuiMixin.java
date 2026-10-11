package net.nyalkan.mixin.profiling;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.nyalkan.Initializer;
import net.nyalkan.render.profiling.FpsOverlay;
import net.nyalkan.render.profiling.ProfilerOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

    @Shadow @Final private DebugScreenOverlay debugOverlay;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void createProfilerOverlay(Minecraft minecraft, CallbackInfo ci) {
        ProfilerOverlay.createInstance(minecraft);
    }

    @Inject(method = "render", at = @At(value = "RETURN"))
    private void renderProfilerOverlay(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (this.debugOverlay.showDebugScreen())
            return;
        if (ProfilerOverlay.shouldRender)
            ProfilerOverlay.INSTANCE.render(guiGraphics);
        else if (Initializer.CONFIG.fpsOverlay)
            FpsOverlay.render(guiGraphics);
    }
}
