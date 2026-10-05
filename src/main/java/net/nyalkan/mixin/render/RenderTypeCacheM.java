package net.nyalkan.mixin.render;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderType.class)
public class RenderTypeCacheM {
    @Unique private static final int SIZE = 64;
    @Unique private static final ResourceLocation[] KEYS = new ResourceLocation[SIZE];
    @Unique private static final RenderType[] VALUES = new RenderType[SIZE];

    @WrapMethod(method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;")
    private static RenderType nyalkan$cachedCutoutNoCull(ResourceLocation location, Operation<RenderType> original) {
        if (location == null || !RenderSystem.isOnRenderThread())
            return original.call(location);

        int slot = System.identityHashCode(location) & (SIZE - 1);
        if (KEYS[slot] == location)
            return VALUES[slot];

        RenderType type = original.call(location);
        KEYS[slot] = location;
        VALUES[slot] = type;
        return type;
    }
}
