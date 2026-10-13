package net.nyalkan.mixin.render.batching;

import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DeltaTracker.Timer.class)
public interface DeltaTrackerTimerAccessor {

    @Accessor("deltaTicks")
    float nyalkan$getDeltaTicks();

    @Accessor("deltaTicks")
    void nyalkan$setDeltaTicks(float value);

    @Accessor("realtimeDeltaTicks")
    float nyalkan$getRealtimeDeltaTicks();

    @Accessor("realtimeDeltaTicks")
    void nyalkan$setRealtimeDeltaTicks(float value);
}
