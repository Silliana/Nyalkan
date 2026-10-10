package net.nyalkan.mixin.render.blockentity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nyalkan.interfaces.ExtendedBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderDispatcher.class)
public class BlockEntityRendererCacheM {
    @Unique private static int nyalkan$epoch;

    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void nyalkan$reload(CallbackInfo ci) {
        nyalkan$epoch++;
    }

    @WrapMethod(method = "getRenderer")
    private <E extends BlockEntity> BlockEntityRenderer<E> nyalkan$cachedRenderer(E blockEntity, Operation<BlockEntityRenderer<E>> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(blockEntity);

        ExtendedBlockEntity extended = (ExtendedBlockEntity) blockEntity;
        int epoch = nyalkan$epoch;
        @SuppressWarnings("unchecked")
        BlockEntityRenderer<E> renderer = (BlockEntityRenderer<E>) extended.nyalkan$renderer(epoch);
        if (renderer == null) {
            renderer = original.call(blockEntity);
            if (renderer != null)
                extended.nyalkan$setRenderer(renderer, epoch);
        }
        return renderer;
    }
}
