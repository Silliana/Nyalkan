package net.vulkanmod.mixin.compat.veil;

import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// forces legacy multi bind
@Mixin(targets = "foundry.veil.api.client.render.ext.VeilMultiBind", remap = false)
public class VeilMultiBindMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vulkanmod$get(CallbackInfoReturnable<Object> cir) {
        try {
            Class<?> multiBindClass = Class.forName("foundry.veil.api.client.render.ext.VeilMultiBind");
            cir.setReturnValue(Enum.valueOf((Class<Enum>) (Class) multiBindClass, "LEGACY"));
        } catch (ReflectiveOperationException e) {
            Initializer.LOGGER.error("[VulkanMod] Failed to force VeilMultiBind.LEGACY", e);
        }
    }
}
