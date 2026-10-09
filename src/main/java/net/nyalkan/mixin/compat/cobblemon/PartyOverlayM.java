package net.nyalkan.mixin.compat.cobblemon;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.nyalkan.render.gui.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

// party portraits 20 hz
@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.client.gui.PartyOverlay", remap = false)
public abstract class PartyOverlayM {
    @Unique private static final HudCache nyalkan$partyCache = new HudCache("party", 20);

    @WrapMethod(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", require = 0)
    private void nyalkan$cachedParty(GuiGraphics graphics, DeltaTracker delta, Operation<Void> original) {
        if (!RenderSystem.isOnRenderThread()) {
            original.call(graphics, delta);
            return;
        }
        nyalkan$partyCache.render(graphics, () -> original.call(graphics, delta));
    }
}
