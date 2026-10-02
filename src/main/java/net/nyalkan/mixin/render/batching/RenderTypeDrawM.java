package net.nyalkan.mixin.render.batching;

import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderType;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// flush before state setup
@Mixin(RenderType.class)
public class RenderTypeDrawM {

    @Inject(method = "draw", at = @At("HEAD"))
    private void nyalkan$flushPending(MeshData meshData, CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.flush();
    }
}
