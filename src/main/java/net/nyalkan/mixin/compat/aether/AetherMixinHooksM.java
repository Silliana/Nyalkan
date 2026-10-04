package net.nyalkan.mixin.compat.aether;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Pseudo
@Mixin(targets = "com.aetherteam.aether.mixin.AetherMixinHooks", remap = false)
public abstract class AetherMixinHooksM {

    @Redirect(method = "isCapeVisible", require = 0,
            at = @At(value = "INVOKE", target = "Ljava/util/stream/StreamSupport;stream(Ljava/util/Spliterator;Z)Ljava/util/stream/Stream;"))
    private static <T> Stream<T> nyalkan$sequential(Spliterator<T> spliterator, boolean parallel) {
        return StreamSupport.stream(spliterator, false);
    }
}
