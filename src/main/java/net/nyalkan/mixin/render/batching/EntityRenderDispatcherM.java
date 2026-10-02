package net.nyalkan.mixin.render.batching;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherM {

    @Inject(method = "render", at = @At("HEAD"))
    private void nyalkan$begin(CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.beginEntity();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void nyalkan$end(CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.endEntity();
    }
}
