package net.vulkanmod.mixin.compat.veil;

import net.vulkanmod.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

// forces legacy staging
@Mixin(targets = "dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer", remap = false)
public class StagingBufferM {

    @Inject(method = "create(J)Ldev/ryanhcode/sable/sublevel/render/staging/StagingBuffer;",
            at = @At("HEAD"), remap = false)
    private static void vulkanmod$forceLegacyType(long size, CallbackInfo ci) {
        try {
            Class<?> stagingBufferClass = Class.forName("dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer");
            Field typeField = stagingBufferClass.getDeclaredField("stagingBufferType");
            typeField.setAccessible(true);
            if (typeField.get(null) == null) {
                Class<?> typeEnumClass = Class.forName("dev.ryanhcode.sable.sublevel.render.staging.StagingBuffer$StagingBufferType");
                Object legacy = Enum.valueOf((Class<Enum>) (Class) typeEnumClass, "LEGACY");
                typeField.set(null, legacy);
            }
        } catch (ReflectiveOperationException e) {
            Initializer.LOGGER.error("[VulkanMod] Failed to force StagingBufferType.LEGACY", e);
        }
    }
}
