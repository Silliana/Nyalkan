package net.nyalkan.mixin.compat.journeymap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "journeymap.client.ui.minimap.DisplayVars", remap = false)
public abstract class DisplayVarsM {

    @WrapMethod(method = "drawInfoLabels", require = 0)
    private void nyalkan$batchLabels(GuiGraphics graphics, MultiBufferSource buffers, long now, Operation<Void> original) {
        ImmediateBatcher batcher = ImmediateBatcher.INSTANCE;
        if (!batcher.beginGuiScope()) {
            original.call(graphics, buffers, now);
            return;
        }

        try {
            original.call(graphics, buffers, now);
        } finally {
            batcher.endGui();
        }
    }
}
