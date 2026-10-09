package net.nyalkan.mixin.compat.cobblemon;

import net.nyalkan.render.immediate.ImmediateBatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// matches every overload
@Pseudo
@Mixin(targets = {"com.cobblemon.mod.common.api.gui.GuiUtilsKt", "com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt"}, remap = false)
public class PortraitBatchM {
    @Inject(method = {"drawPosablePortrait*", "drawProfilePokemon*"}, at = @At("HEAD"), require = 0)
    private static void nyalkan$suspend(CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.suspend();
    }

    @Inject(method = {"drawPosablePortrait*", "drawProfilePokemon*"}, at = @At("RETURN"), require = 0)
    private static void nyalkan$resume(CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.resume();
    }
}
