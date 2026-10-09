package net.nyalkan.mixin.render.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.render.model.BackfaceCull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemQuadCullM {
    @Inject(method = "renderQuadList", at = @At("HEAD"))
    private void nyalkan$prepare(PoseStack poseStack, VertexConsumer buffer, List<BakedQuad> quads, ItemStack stack, int light, int overlay, CallbackInfo ci) {
        BackfaceCull.prepareQuads(poseStack.last().pose());
    }

    @WrapOperation(method = "renderQuadList", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;putBulkData(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lnet/minecraft/client/renderer/block/model/BakedQuad;FFFFIIZ)V"))
    private void nyalkan$cull(VertexConsumer buffer, PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light, int overlay, boolean readColor, Operation<Void> original) {
        if (buffer instanceof ExtendedVertexBuilder builder && !BackfaceCull.quadVisible(builder, quad.getVertices()))
            return;
        original.call(buffer, pose, quad, r, g, b, a, light, overlay, readColor);
    }
}
