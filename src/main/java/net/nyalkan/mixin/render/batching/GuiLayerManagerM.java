package net.nyalkan.mixin.render.batching;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import net.nyalkan.Initializer;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GuiLayerManager.class, remap = false)
public class GuiLayerManagerM {

    @WrapMethod(method = "renderInner")
    private void nyalkan$batchLayers(GuiGraphics guiGraphics, DeltaTracker partialTick, Operation<Void> original) {
        if (!Initializer.CONFIG.hudBatching || !ImmediateBatcher.isEnabled()) {
            original.call(guiGraphics, partialTick);
            return;
        }

        ImmediateBatcher batcher = ImmediateBatcher.INSTANCE;
        batcher.beginGui();
        try {
            original.call(guiGraphics, partialTick);
        } finally {
            batcher.endGui();
        }
    }

    @Inject(method = "renderInner", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"))
    private void nyalkan$flushLayer(GuiGraphics guiGraphics, DeltaTracker partialTick, CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.flushGui();
    }
}
