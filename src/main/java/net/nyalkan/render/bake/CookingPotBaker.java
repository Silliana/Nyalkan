package net.nyalkan.render.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;

// static pot body only
public final class CookingPotBaker implements BlockEntityBaker<BlockEntity, CookingPotBaker.Key> {
    public static final String BLOCK_ENTITY = "com.cobblemon.mod.common.block.entity.CampfireBlockEntity";
    private static final String RENDERER = "com.cobblemon.mod.common.client.render.block.CampfireBlockEntityRenderer";

    public record Key(BlockState pot, Direction facing, int broth) {}

    private volatile MethodHandle[] getters;

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public Key key(BlockEntity blockEntity, BlockState state, BlockEntityRenderer<BlockEntity> renderer) {
        if (!renderer.getClass().getName().equals(RENDERER))
            return null;

        Property<?> lid = state.getBlock().getStateDefinition().getProperty("lid");
        Property<?> itemFacing = state.getBlock().getStateDefinition().getProperty("item_facing");
        if (lid == null || itemFacing == null ||!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
            return null;

        try {
            MethodHandle[] getters = getters(blockEntity.getClass());
            if (!(((ItemStack) getters[0].invoke(blockEntity)).getItem() instanceof BlockItem item))
                return null;

            BlockState pot = item.getBlock().defaultBlockState();
            if (!pot.hasProperty(BlockStateProperties.OPEN) || !pot.hasProperty(BlockStateProperties.OCCUPIED) || !pot.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                return null;

            Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
            float rotation = (((Direction) state.getValue(itemFacing)).getOpposite().toYRot() + facing.toYRot()) % 360;
            boolean occupied = !((List<?>) getters[2].invoke(blockEntity)).isEmpty() || !((List<?>) getters[3].invoke(blockEntity)).isEmpty();
            pot = pot.setValue(BlockStateProperties.OPEN, !(Boolean) state.getValue(lid))
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.fromYRot(rotation))
                    .setValue(BlockStateProperties.OCCUPIED, occupied);
            return new Key(pot, facing, (int) getters[1].invoke(blockEntity));
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public BakedMesh bake(Key key) {
        float angle = switch (key.facing) {
            case NORTH -> 180.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        poseStack.translate(0.0F, 0.4375F, 0.0F);
        return BlockModelQuads.mesh(key.pot, poseStack, key.broth).keepLive();
    }

    private MethodHandle[] getters(Class<?> type) throws ReflectiveOperationException {
        MethodHandle[] getters = this.getters;
        if (getters == null) {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            getters = new MethodHandle[] {
                    lookup.findVirtual(type, "getPotItem", MethodType.methodType(ItemStack.class)),
                    lookup.findVirtual(type, "getBrothColor", MethodType.methodType(int.class)),
                    lookup.findVirtual(type, "getSeasonings", MethodType.methodType(List.class)),
                    lookup.findVirtual(type, "getIngredients", MethodType.methodType(List.class))
            };
            this.getters = getters;
        }
        return getters;
    }
}
