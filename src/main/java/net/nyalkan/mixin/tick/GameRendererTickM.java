package net.nyalkan.mixin.tick;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.nyalkan.tick.TickSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererTickM {

    @Inject(method = "render", at = @At("HEAD"))
    private void nyalkan$spreadTicks(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        TickSpreader.frame();
    }
}
