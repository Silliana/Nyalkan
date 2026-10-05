package net.nyalkan.mixin.render.batching;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChatScreen.class)
public class ChatScreenM {

    @WrapMethod(method = "render")
    private void nyalkan$batchChat(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
        ImmediateBatcher batcher = ImmediateBatcher.INSTANCE;
        if (!batcher.beginGuiScope()) {
            original.call(graphics, mouseX, mouseY, partialTick);
            return;
        }

        try {
            original.call(graphics, mouseX, mouseY, partialTick);
        } finally {
            batcher.endGui();
        }
    }
}
