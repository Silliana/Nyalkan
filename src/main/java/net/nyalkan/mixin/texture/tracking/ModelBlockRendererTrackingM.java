package net.nyalkan.mixin.texture.tracking;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.nyalkan.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModelBlockRenderer.class)
public class ModelBlockRendererTrackingM {

    @Inject(method = "putQuadData", at = @At("HEAD"))
    private void nyalkan$markSprite(BlockAndTintGetter level, BlockState state, BlockPos pos, VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                      float b0, float b1, float b2, float b3, int l0, int l1, int l2, int l3, int overlay, CallbackInfo ci) {
        SpriteUtil.markActive(quad.getSprite());
    }
}
