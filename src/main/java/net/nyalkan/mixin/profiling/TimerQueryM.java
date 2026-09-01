package net.nyalkan.mixin.profiling;

import com.mojang.blaze3d.systems.TimerQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

// no gl timer query
@Mixin(TimerQuery.class)
public class TimerQueryM {

    @Overwrite
    public static Optional<TimerQuery> getInstance() {
        return Optional.empty();
    }
}
