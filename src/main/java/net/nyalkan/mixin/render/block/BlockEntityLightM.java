package net.nyalkan.mixin.render.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.nyalkan.render.chunk.build.BlockEntityLightCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityLightM {
    @WrapOperation(method = "setupAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    private static int nyalkan$cachedLight(BlockAndTintGetter level, BlockPos pos, Operation<Integer> original) {
        if (!(level instanceof ClientLevel clientLevel) || !RenderSystem.isOnRenderThread())
            return original.call(level, pos);
        return BlockEntityLightCache.get(clientLevel, pos, () -> original.call(level, pos));
    }
}
