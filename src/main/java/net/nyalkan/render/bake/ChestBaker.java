package net.nyalkan.render.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.nyalkan.interfaces.AnimatedLid;
import net.nyalkan.mixin.render.block.ChestLidControllerAccessor;
import net.nyalkan.mixin.render.block.ChestRendererAccessor;
import net.nyalkan.render.vertex.TerrainRenderType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

// closed chests only
public final class ChestBaker implements BlockEntityBaker<BlockEntity, ChestBaker.Key> {
    public record Key(ModelLayerLocation layer, Direction facing, ResourceLocation texture) {}

    private static final ClassValue<Boolean> TEXTURE_ONLY = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            if (!ChestRenderer.class.isAssignableFrom(type))
                return false;
            for (Class<?> c = type; c != ChestRenderer.class; c = c.getSuperclass()) {
                for (Method method : c.getDeclaredMethods()) {
                    if (method.getName().equals("render") && method.getParameterCount() == 6)
                        return false;
                }
            }
            return true;
        }
    };

    @Nullable
    @Override
    public Key key(BlockEntity blockEntity, BlockState state, BlockEntityRenderer<BlockEntity> renderer) {
        if (!TEXTURE_ONLY.get(renderer.getClass()) || !(state.getBlock() instanceof AbstractChestBlock<?>)
                || !state.hasProperty(ChestBlock.FACING) || ((AnimatedLid) blockEntity).nyalkan$lidAnimating())
            return null;

        ChestType chestType = state.hasProperty(ChestBlock.TYPE) ? state.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        ModelLayerLocation layer = switch (chestType) {
            case SINGLE -> ModelLayers.CHEST;
            case LEFT -> ModelLayers.DOUBLE_CHEST_LEFT;
            case RIGHT -> ModelLayers.DOUBLE_CHEST_RIGHT;
        };
        ResourceLocation texture = ((ChestRendererAccessor) renderer).nyalkan$getMaterial(blockEntity, chestType).texture();
        return new Key(layer, state.getValue(ChestBlock.FACING), texture);
    }

    @Nullable
    @Override
    public BakedMesh bake(Key key) {
        TextureAtlasSprite sprite = ModelPartQuads.blockSprite(key.texture);
        if (sprite == null)
            return null;

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-key.facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        return BakedMesh.of(TerrainRenderType.CUTOUT, ModelPartQuads.bake(key.layer, poseStack, sprite));
    }

    public static void refreshLid(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (level == null || !level.isClientSide)
            return;

        AnimatedLid lid = (AnimatedLid) blockEntity;
        ChestLidController controller = lid.nyalkan$lid();
        boolean animating = ((ChestLidControllerAccessor) controller).nyalkan$shouldBeOpen()
                || controller.getOpenness(0.0F) > 0.0F || controller.getOpenness(1.0F) > 0.0F;
        if (animating == lid.nyalkan$lidAnimating())
            return;

        lid.nyalkan$setLidAnimating(animating);
        BlockEntityBakery.invalidate(blockEntity);
    }
}
