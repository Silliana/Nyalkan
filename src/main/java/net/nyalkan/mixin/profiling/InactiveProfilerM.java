package net.nyalkan.mixin.profiling;

import net.minecraft.util.profiling.InactiveProfiler;
import net.nyalkan.render.profiling.DebugLabels;
import net.nyalkan.vulkan.Vulkan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(InactiveProfiler.class)
public class InactiveProfilerM {

    @Inject(method = "push(Ljava/lang/String;)V", at = @At("HEAD"))
    private void nyalkan$push(String name, CallbackInfo ci) {
        if (Vulkan.DEBUG_LABELS) DebugLabels.push(name);
    }

    @Inject(method = "push(Ljava/util/function/Supplier;)V", at = @At("HEAD"))
    private void nyalkan$pushSupplier(Supplier<String> name, CallbackInfo ci) {
        if (Vulkan.DEBUG_LABELS) DebugLabels.push(name.get());
    }

    @Inject(method = "pop", at = @At("HEAD"))
    private void nyalkan$pop(CallbackInfo ci) {
        if (Vulkan.DEBUG_LABELS) DebugLabels.pop();
    }

    @Inject(method = "popPush(Ljava/lang/String;)V", at = @At("HEAD"))
    private void nyalkan$popPush(String name, CallbackInfo ci) {
        if (Vulkan.DEBUG_LABELS) {
            DebugLabels.pop();
            DebugLabels.push(name);
        }
    }

    @Inject(method = "popPush(Ljava/util/function/Supplier;)V", at = @At("HEAD"))
    private void nyalkan$popPushSupplier(Supplier<String> name, CallbackInfo ci) {
        if (Vulkan.DEBUG_LABELS) {
            DebugLabels.pop();
            DebugLabels.push(name.get());
        }
    }
}
