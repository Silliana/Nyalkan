package net.nyalkan.mixin.compat.cobblemon;

import net.nyalkan.render.Nanos;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;

@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.client.sound.SoundTracker", remap = false)
public abstract class SoundTrackerM {
    @Unique private static final long RETRY = Nanos.SECOND;
    @Unique private static final HashMap<Object, HashMap<ResourceLocation, Long>> nyalkan$attempts = new HashMap<>();

    @WrapMethod(method = "play", require = 0)
    private void nyalkan$throttledPlay(Object owner, SoundInstance sound, Operation<Void> original) {
        if (!this.getClass().getName().endsWith("BlockEntitySoundTracker")) {
            original.call(owner, sound);
            return;
        }

        long now = System.nanoTime();
        if (nyalkan$attempts.size() > 4096)
            nyalkan$attempts.clear();
        HashMap<ResourceLocation, Long> sounds = nyalkan$attempts.computeIfAbsent(owner, o -> new HashMap<>());
        Long last = sounds.get(sound.getLocation());
        if (last != null && now - last < RETRY)
            return;
        sounds.put(sound.getLocation(), now);
        original.call(owner, sound);
    }
}
