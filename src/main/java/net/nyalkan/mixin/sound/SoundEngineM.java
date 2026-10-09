package net.nyalkan.mixin.sound;

import net.nyalkan.render.Nanos;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.audio.ListenerTransform;
import net.minecraft.client.Camera;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundEngineExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SoundEngine.class)
public class SoundEngineM {
    @Unique private static final long nyalkan$INTERVAL = Nanos.SECOND / 60;
    @Unique private ListenerTransform nyalkan$sent;
    @Unique private long nyalkan$sentAt;

    // fewer sound thread wakeups
    @WrapOperation(method = "updateSource", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngineExecutor;execute(Ljava/lang/Runnable;)V"))
    private void nyalkan$throttle(SoundEngineExecutor executor, Runnable task, Operation<Void> original, Camera camera, @Local ListenerTransform transform) {
        long now = System.nanoTime();
        if (transform.equals(this.nyalkan$sent) || now - this.nyalkan$sentAt < nyalkan$INTERVAL)
            return;
        this.nyalkan$sent = transform;
        this.nyalkan$sentAt = now;
        original.call(executor, task);
    }
}
