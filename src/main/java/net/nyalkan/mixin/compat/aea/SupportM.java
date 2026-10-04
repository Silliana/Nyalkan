package net.nyalkan.mixin.compat.aea;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.WeakHashMap;

@Pseudo
@Mixin(targets = "de.dafuqs.additionalentityattributes.Support", remap = false)
public abstract class SupportM {
    @Unique private static final Map<LivingEntity, double[]> WIDTHS = new WeakHashMap<>();
    @Unique private static final Map<LivingEntity, double[]> HEIGHTS = new WeakHashMap<>();

    @WrapMethod(method = "getModelWidth", require = 0)
    private static double nyalkan$width(LivingEntity entity, double base, Operation<Double> original) {
        return nyalkan$cached(WIDTHS, entity, base, original);
    }

    @WrapMethod(method = "getModelHeight", require = 0)
    private static double nyalkan$height(LivingEntity entity, double base, Operation<Double> original) {
        return nyalkan$cached(HEIGHTS, entity, base, original);
    }

    @Unique
    private static double nyalkan$cached(Map<LivingEntity, double[]> cache, LivingEntity entity, double base, Operation<Double> original) {
        if (entity == null || !RenderSystem.isOnRenderThread())
            return original.call(entity, base);

        double[] entry = cache.get(entity);
        if (entry != null && entry[0] == entity.tickCount && entry[1] == base)
            return entry[2];

        double value = original.call(entity, base);
        if (entry == null)
            cache.put(entity, new double[]{entity.tickCount, base, value});
        else {
            entry[0] = entity.tickCount;
            entry[1] = base;
            entry[2] = value;
        }
        return value;
    }
}
