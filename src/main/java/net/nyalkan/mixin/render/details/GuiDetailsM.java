package net.nyalkan.mixin.render.details;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.nyalkan.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiDetailsM {

    @Inject(method = "renderVignette", at = @At("HEAD"), cancellable = true)
    private void nyalkan$vignette(GuiGraphics guiGraphics, Entity entity, CallbackInfo ci) {
        if (!Initializer.CONFIG.vignette)
            ci.cancel();
    }
}
