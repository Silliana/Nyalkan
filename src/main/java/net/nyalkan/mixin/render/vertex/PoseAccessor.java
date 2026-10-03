package net.nyalkan.mixin.render.vertex;

import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PoseStack.Pose.class)
public interface PoseAccessor {

    @Accessor("trustedNormals")
    boolean nyalkan$trustedNormals();

    @Accessor("trustedNormals")
    void nyalkan$setTrustedNormals(boolean trustedNormals);
}
