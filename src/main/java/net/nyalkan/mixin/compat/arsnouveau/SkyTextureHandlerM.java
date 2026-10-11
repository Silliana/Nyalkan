package net.nyalkan.mixin.compat.arsnouveau;

import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.nyalkan.render.compat.SkyTargetGate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// skip unused sky pass
@Pseudo
@Mixin(targets = "com.hollingsworth.arsnouveau.client.SkyTextureHandler", remap = false)
public abstract class SkyTextureHandlerM {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nyalkan$skipUnusedSky(RenderLevelStageEvent event, CallbackInfo ci) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY && !SkyTargetGate.shouldRender())
            ci.cancel();
    }
}
