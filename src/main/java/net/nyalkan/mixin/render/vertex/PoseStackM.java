package net.nyalkan.mixin.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import net.nyalkan.Initializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayDeque;
import java.util.Deque;

// reuse popped poses
@Mixin(PoseStack.class)
public class PoseStackM {

    @Unique private ArrayDeque<PoseStack.Pose> nyalkan$freePoses;

    @Redirect(method = "pushPose", at = @At(value = "NEW", target = "(Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/PoseStack$Pose;"))
    private PoseStack.Pose nyalkan$newPose(PoseStack.Pose parent) {
        if (!Initializer.CONFIG.poseReuse || this.nyalkan$freePoses == null || this.nyalkan$freePoses.isEmpty())
            return parent.copy();

        PoseStack.Pose pose = this.nyalkan$freePoses.pop();
        pose.pose().set(parent.pose());
        pose.normal().set(parent.normal());
        ((PoseAccessor) (Object) pose).nyalkan$setTrustedNormals(((PoseAccessor) (Object) parent).nyalkan$trustedNormals());
        return pose;
    }

    @Redirect(method = "popPose", at = @At(value = "INVOKE", target = "Ljava/util/Deque;removeLast()Ljava/lang/Object;"))
    private Object nyalkan$recyclePose(Deque<PoseStack.Pose> stack) {
        PoseStack.Pose pose = stack.removeLast();

        if (Initializer.CONFIG.poseReuse) {
            if (this.nyalkan$freePoses == null)
                this.nyalkan$freePoses = new ArrayDeque<>();
            this.nyalkan$freePoses.push(pose);
        }

        return pose;
    }
}
