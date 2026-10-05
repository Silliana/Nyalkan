package net.nyalkan.mixin.compat.journeymap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "journeymap.client.ui.minimap.MiniMap", remap = false)
public abstract class MiniMapM {

    @WrapMethod(method = "drawOnMapEntities", require = 0)
    private void nyalkan$batchEntities(GuiGraphics graphics, MultiBufferSource buffers, double fontScale, Operation<Void> original) {
        ImmediateBatcher batcher = ImmediateBatcher.INSTANCE;
        if (!batcher.beginGuiScope()) {
            original.call(graphics, buffers, fontScale);
            return;
        }

        try {
            original.call(graphics, buffers, fontScale);
        } finally {
            batcher.endGui();
        }
    }

    @Inject(method = "drawOnMapEntities", require = 0,
            at = @At(value = "INVOKE", target = "Ljourneymap/client/model/map/MapState;getDrawSteps()Ljava/util/List;"))
    private void nyalkan$flushPass(GuiGraphics graphics, MultiBufferSource buffers, double fontScale, CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.flushGui();
    }
}
