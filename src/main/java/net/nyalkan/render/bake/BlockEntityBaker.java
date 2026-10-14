package net.nyalkan.render.bake;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface BlockEntityBaker<T extends BlockEntity, K> {
    @Nullable
    K key(T blockEntity, BlockState state, BlockEntityRenderer<T> renderer);

    @Nullable
    BakedMesh bake(K key);
}
