package net.nyalkan.render.model;

import net.nyalkan.render.Nanos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedModelPart;
import net.nyalkan.render.DistanceScale;

import java.util.Map;
import java.util.WeakHashMap;

public final class AnimationLod {
    private static final double NEAR = 12.0;
    private static final double MID = 24.0;
    private static final long MID_INTERVAL = Nanos.SECOND / 60;
    private static final long FAR_INTERVAL = Nanos.SECOND / 20;

    private static final Map<Entity, Snapshot> SNAPSHOTS = new WeakHashMap<>();

    private static final class Snapshot {
        Object model;
        ModelPart root;
        long time;
        float[] state = new float[0];
    }

    private AnimationLod() {}

    public static void animate(Entity entity, Object model, ModelPart root, Runnable setupAnim) {
        if (!Initializer.CONFIG.animationLod || root == null || entity.level() == null) {
            setupAnim.run();
            return;
        }

        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double distance = entity.distanceToSqr(camera);
        if (distance < DistanceScale.sqr(NEAR)) {
            setupAnim.run();
            return;
        }

        long interval = distance < DistanceScale.sqr(MID) ? MID_INTERVAL : FAR_INTERVAL;
        long now = System.nanoTime();
        Snapshot snapshot = SNAPSHOTS.get(entity);
        ExtendedModelPart part = (ExtendedModelPart) (Object) root;

        boolean same = snapshot != null && snapshot.model == model && snapshot.root == root;
        if (same && now - snapshot.time < interval) {
            try {
                part.nyalkan$readState(snapshot.state, 0);
                return;
            } catch (ArrayIndexOutOfBoundsException e) {
                same = false;
            }
        }

        setupAnim.run();

        if (snapshot == null) {
            snapshot = new Snapshot();
            SNAPSHOTS.put(entity, snapshot);
        }
        if (!same)
            snapshot.state = new float[part.nyalkan$stateSize()];
        part.nyalkan$writeState(snapshot.state, 0);
        snapshot.model = model;
        snapshot.root = root;
        snapshot.time = now;
    }
}
