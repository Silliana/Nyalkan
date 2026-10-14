package net.nyalkan.render.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.nyalkan.render.vertex.TerrainRenderType;
import org.jetbrains.annotations.Nullable;

public final class BedBaker implements BlockEntityBaker<BedBlockEntity, BedBaker.Key> {
    public record Key(ModelLayerLocation layer, Direction facing, ResourceLocation texture) {}

    @Nullable
    @Override
    public Key key(BedBlockEntity blockEntity, BlockState state, BlockEntityRenderer<BedBlockEntity> renderer) {
        if (renderer.getClass() != BedRenderer.class || !(state.getBlock() instanceof BedBlock))
            return null;

        ModelLayerLocation layer = state.getValue(BedBlock.PART) == BedPart.HEAD ? ModelLayers.BED_HEAD : ModelLayers.BED_FOOT;
        ResourceLocation texture = Sheets.BED_TEXTURES[blockEntity.getColor().getId()].texture();
        return new Key(layer, state.getValue(BedBlock.FACING), texture);
    }

    @Nullable
    @Override
    public BakedMesh bake(Key key) {
        TextureAtlasSprite sprite = ModelPartQuads.blockSprite(key.texture);
        if (sprite == null)
            return null;

        PoseStack poseStack = new PoseStack();
        poseStack.translate(0.0F, 0.5625F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F + key.facing.toYRot()));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        return BakedMesh.of(TerrainRenderType.SOLID, ModelPartQuads.bake(key.layer, poseStack, sprite));
    }
}
