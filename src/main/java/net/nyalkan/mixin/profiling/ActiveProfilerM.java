package net.nyalkan.mixin.profiling;

import net.minecraft.util.profiling.ActiveProfiler;
import net.nyalkan.render.profiling.DebugLabels;
import net.nyalkan.vulkan.Vulkan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ActiveProfiler.class)
public class ActiveProfilerM {

    @Inject(method = "push(Ljava/lang/String;)V", at = @At("HEAD"))
    private void nyalkan$push(String name, CallbackInfo ci) {
        if (Vulkan.DEBUG_LABELS) DebugLabels.push(name);
    }

    @Inject(method = "pop", at = @At("HEAD"))
    private void nyalkan$pop(CallbackInfo ci) {
        if (Vulkan.DEBUG_LABELS) DebugLabels.pop();
    }
}
