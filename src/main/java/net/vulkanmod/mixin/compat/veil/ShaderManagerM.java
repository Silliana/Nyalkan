package net.vulkanmod.mixin.compat.veil;

import net.vulkanmod.Initializer;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.FunctionProvider;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@Mixin(targets = "foundry.veil.api.client.render.shader.ShaderManager", remap = false)
public class ShaderManagerM {

    private static Constructor<GLCapabilities> constructor;

    @SuppressWarnings("unchecked")
    @Redirect(method = {"createDynamicProgram", "prepare"}, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;supplyAsync(Ljava/util/function/Supplier;Ljava/util/concurrent/Executor;)Ljava/util/concurrent/CompletableFuture;", remap = false))
    private CompletableFuture<?> vulkanmod$supplyAsync(Supplier<?> supplier, Executor executor) {
        try {
            if (constructor == null) {
                constructor = GLCapabilities.class.getDeclaredConstructor(FunctionProvider.class, Set.class, boolean.class, IntFunction.class);
                constructor.setAccessible(true);
            }
            FunctionProvider fp = buffer -> 0L;
            IntFunction<org.lwjgl.PointerBuffer> pointers = size -> MemoryUtil.memAllocPointer(Math.max(size, 1));
            return CompletableFuture.completedFuture(constructor.newInstance(fp, Collections.emptySet(), false, pointers));
        } catch (ReflectiveOperationException e) {
            Initializer.LOGGER.error("[VulkanMod] Failed to fake GLCapabilities", e);
            return CompletableFuture.completedFuture(null);
        }
    }
}
