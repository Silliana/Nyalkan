package net.nyalkan.render.bake;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedBlockEntity;
import net.nyalkan.render.chunk.WorldRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// static block entity meshes
public final class BlockEntityBakery {
    private static final BakedMesh NONE = new BakedMesh(Map.of(), false);
    private static final Map<Class<?>, Baker<?, ?>> BY_CLASS = new ConcurrentHashMap<>();
    private static final Map<String, Baker<?, ?>> BY_NAME = new ConcurrentHashMap<>();
    private static final ClassValue<Baker<?, ?>> BAKERS = new ClassValue<>() {
        @Override
        protected Baker<?, ?> computeValue(Class<?> type) {
            Baker<?, ?> baker = BY_CLASS.get(type);
            return baker != null ? baker : BY_NAME.get(type.getName());
        }
    };

    static {
        ChestBaker chest = new ChestBaker();
        register(ChestBlockEntity.class, chest);
        register(TrappedChestBlockEntity.class, chest);
        register(EnderChestBlockEntity.class, chest);
        register("noobanidus.mods.lootr.common.block.entity.LootrChestBlockEntity", chest);
        register("noobanidus.mods.lootr.common.block.entity.LootrTrappedChestBlockEntity", chest);
        register(BedBlockEntity.class, new BedBaker());
        register(CookingPotBaker.BLOCK_ENTITY, new CookingPotBaker());
    }

    private static boolean skipBaked;

    private BlockEntityBakery() {}

    public static <T extends BlockEntity> void register(Class<? extends T> type, BlockEntityBaker<T, ?> baker) {
        BY_CLASS.put(type, new Baker<>(baker));
    }

    public static void register(String className, BlockEntityBaker<?, ?> baker) {
        BY_NAME.put(className, new Baker<>(baker));
    }

    public static void clear() {
        BY_CLASS.values().forEach(baker -> baker.meshes.clear());
        BY_NAME.values().forEach(baker -> baker.meshes.clear());
    }

    @Nullable
    public static BakedMesh get(BlockEntity blockEntity, BlockState state, BlockEntityRenderer<?> renderer) {
        if (!Initializer.CONFIG.bakedBlockEntities)
            return null;
        Baker<?, ?> baker = BAKERS.get(blockEntity.getClass());
        return baker != null ? baker.get(blockEntity, state, renderer) : null;
    }

    public static void refresh(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        Baker<?, ?> baker = BAKERS.get(blockEntity.getClass());
        if (baker == null || level == null || !level.isClientSide || !Initializer.CONFIG.bakedBlockEntities)
            return;

        BlockEntityRenderer<?> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(blockEntity);
        Object key = renderer != null ? baker.key(blockEntity, blockEntity.getBlockState(), renderer) : null;
        if (!Objects.equals(key, ((ExtendedBlockEntity) blockEntity).nyalkan$bakeKey()))
            invalidate(blockEntity);
    }

    public static boolean skipBaked() {
        return skipBaked;
    }

    public static void setSkipBaked(boolean skip) {
        skipBaked = skip;
    }

    public static void invalidate(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide)
            return;

        WorldRenderer worldRenderer = WorldRenderer.getInstance();
        if (Initializer.CONFIG.bakedBlockEntities && worldRenderer != null && worldRenderer.getSectionGrid() != null) {
            BlockPos pos = blockEntity.getBlockPos();
            worldRenderer.setSectionDirty(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4, true);
        }
    }

    private static final class Baker<T extends BlockEntity, K> {
        final BlockEntityBaker<T, K> baker;
        final Map<K, BakedMesh> meshes = new ConcurrentHashMap<>();

        Baker(BlockEntityBaker<T, K> baker) {
            this.baker = baker;
        }

        @SuppressWarnings("unchecked")
        @Nullable
        K key(BlockEntity blockEntity, BlockState state, BlockEntityRenderer<?> renderer) {
            return this.baker.key((T) blockEntity, state, (BlockEntityRenderer<T>) renderer);
        }

        @Nullable
        BakedMesh get(BlockEntity blockEntity, BlockState state, BlockEntityRenderer<?> renderer) {
            K key = key(blockEntity, state, renderer);
            ((ExtendedBlockEntity) blockEntity).nyalkan$setBakeKey(key);
            if (key == null)
                return null;

            BakedMesh mesh = this.meshes.get(key);
            if (mesh == null) {
                mesh = this.baker.bake(key);
                this.meshes.putIfAbsent(key, mesh != null ? mesh : NONE);
            }
            return mesh == NONE || mesh == null || mesh.layers().isEmpty() ? null : mesh;
        }
    }
}
