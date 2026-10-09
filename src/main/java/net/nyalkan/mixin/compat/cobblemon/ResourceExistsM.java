package net.nyalkan.mixin.compat.cobblemon;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.resources.ResourceLocation;
import net.nyalkan.render.compat.ResourceExistsCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.client.util.ClientDistributionUtilsKt", remap = false)
public class ResourceExistsM {
    @WrapMethod(method = "exists(Lnet/minecraft/resources/ResourceLocation;)Z", require = 0)
    private static boolean nyalkan$cached(ResourceLocation location, Operation<Boolean> original) {
        return ResourceExistsCache.exists(location, original::call);
    }
}
