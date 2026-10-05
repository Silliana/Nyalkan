package net.nyalkan.mixin.render.entity.layer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;

@Mixin(VillagerProfessionLayer.class)
public class VillagerProfessionLayerM {
    @Unique private final Map<String, Map<ResourceLocation, ResourceLocation>> nyalkan$locations = new HashMap<>();

    @WrapMethod(method = "getResourceLocation")
    private ResourceLocation nyalkan$cachedLocation(String folder, ResourceLocation location, Operation<ResourceLocation> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(folder, location);

        Map<ResourceLocation, ResourceLocation> byName = this.nyalkan$locations.computeIfAbsent(folder, f -> new HashMap<>());
        ResourceLocation cached = byName.get(location);
        if (cached == null) {
            cached = original.call(folder, location);
            byName.put(location, cached);
        }
        return cached;
    }
}
