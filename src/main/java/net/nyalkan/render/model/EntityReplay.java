package net.nyalkan.render.model;

import net.nyalkan.render.Nanos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedEntity;
import net.nyalkan.render.DistanceScale;
import org.joml.Matrix4f;

public final class EntityReplay {
    private static final double NEAR = 24.0;
    private static final double MID = 48.0;
    private static final long MID_INTERVAL = Nanos.SECOND / 30;
    private static final long FAR_INTERVAL = Nanos.SECOND / 15;

    private EntityReplay() {}

    public static void render(EntityRenderDispatcher dispatcher, Entity entity, double x, double y, double z, float yaw, float partialTick,
                              PoseStack poseStack, MultiBufferSource source, int light) {
        double distance = x * x + y * y + z * z;
        ExtendedEntity extended = (ExtendedEntity) entity;
        if (distance < DistanceScale.sqr(NEAR) || !Initializer.CONFIG.entityReplay || !OutputReplay.usable(entity.getClass(), source, poseStack)) {
            extended.nyalkan$setReplay(null);
            dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, source, light);
            return;
        }

        Matrix4f pose = poseStack.last().pose();
        long interval = distance < DistanceScale.sqr(MID) ? MID_INTERVAL : FAR_INTERVAL;
        extended.nyalkan$setReplay(OutputReplay.render(extended.nyalkan$replay(), entity.getClass(), null, interval,
                (float) (x + pose.m30()), (float) (y + pose.m31()), (float) (z + pose.m32()), source, false,
                recorder -> dispatcher.render(entity, x, y, z, yaw, partialTick, poseStack, recorder, light)));
    }
}
