package net.nyalkan.mixin.render.entity.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.render.model.ModelReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(HierarchicalModel.class)
public abstract class HierarchicalModelM {

    @Shadow public abstract ModelPart root();

    @Unique private final ModelReplay nyalkan$replay = new ModelReplay();
    @Unique private final Map<String, Optional<ModelPart>> nyalkan$descendants = new HashMap<>();
    @Unique private ModelPart nyalkan$descendantRoot;

    @WrapMethod(method = "getAnyDescendantWithName")
    private Optional<ModelPart> nyalkan$cachedDescendant(String name, Operation<Optional<ModelPart>> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(name);

        ModelPart root = this.root();
        if (root != this.nyalkan$descendantRoot) {
            this.nyalkan$descendants.clear();
            this.nyalkan$descendantRoot = root;
        }

        Optional<ModelPart> part = this.nyalkan$descendants.get(name);
        if (part == null) {
            part = original.call(name);
            this.nyalkan$descendants.put(name, part);
        }
        return part;
    }

    @WrapMethod(method = "renderToBuffer")
    private void nyalkan$replayPass(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color, Operation<Void> original) {
        ExtendedVertexBuilder builder = ExtendedVertexBuilder.of(buffer);
        if (!Initializer.CONFIG.modelReplay || builder == null || !builder.canBulkWriteEntity() || builder.bulkSprite() != null) {
            original.call(poseStack, buffer, light, overlay, color);
            return;
        }

        ModelPart root = this.root();
        PoseStack.Pose pose = poseStack.last();
        if (this.nyalkan$replay.tryReplay(root, pose, builder, light, overlay, color))
            return;

        int verticesBefore = builder.bulkVertexCount();
        long bulkBefore = ModelReplay.bulkVertices;

        original.call(poseStack, buffer, light, overlay, color);

        int produced = builder.bulkVertexCount() - verticesBefore;
        if (produced > 0 && produced == ModelReplay.bulkVertices - bulkBefore) {
            long start = builder.bulkWriteEnd() - (long) produced * 36;
            this.nyalkan$replay.record(root, poseStack.last(), start, produced, light, overlay, color);
        } else {
            this.nyalkan$replay.invalidate();
        }
    }
}
