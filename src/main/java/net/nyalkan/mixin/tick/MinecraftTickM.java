package net.nyalkan.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.sounds.SoundManager;
import net.nyalkan.tick.FrameBudget;
import net.nyalkan.tick.TickSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftTickM {

    @WrapOperation(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V"))
    private void nyalkan$budgetTasks(Minecraft minecraft, Operation<Void> original, @Local(ordinal = 0) int ticks) {
        FrameBudget.runTasks(minecraft, ticks);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;fireClientTickPre()V", remap = false))
    private void nyalkan$flushBeforeTick(Operation<Void> original) {
        TickSpreader.runTasks();
        original.call();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/ClientHooks;fireClientTickPost()V", remap = false))
    private void nyalkan$deferTickPost(Operation<Void> original) {
        TickSpreader.deferOrRun(original::call);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/TextureManager;tick()V"))
    private void nyalkan$deferTextures(TextureManager manager, Operation<Void> original) {
        TickSpreader.deferOrRun(() -> original.call(manager));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;tick(Z)V"))
    private void nyalkan$deferSound(SoundManager manager, boolean paused, Operation<Void> original) {
        TickSpreader.deferOrRun(() -> original.call(manager, paused));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;animateTick(III)V"))
    private void nyalkan$deferAmbient(ClientLevel level, int x, int y, int z, Operation<Void> original) {
        TickSpreader.deferOrRun(() -> {
            if (Minecraft.getInstance().level == level)
                original.call(level, x, y, z);
        });
    }
}
