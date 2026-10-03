package net.nyalkan.mixin.texture.tracking;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.nyalkan.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemRendererTrackingM {

    @Inject(method = "renderQuadList", at = @At("HEAD"))
    private void nyalkan$markSprites(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads, ItemStack stack, int light, int overlay, CallbackInfo ci) {
        for (int i = 0; i < quads.size(); i++)
            SpriteUtil.markActive(quads.get(i).getSprite());
    }
}
