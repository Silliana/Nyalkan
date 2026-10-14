package net.nyalkan.render.model;

import net.nyalkan.render.Nanos;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedBlockEntity;
import net.nyalkan.render.DistanceScale;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockEntityReplay {
    private static final double NEAR = 16.0;
    private static final double MID = 32.0;
    private static final long NEAR_INTERVAL = Nanos.SECOND / 60;
    private static final long MID_INTERVAL = Nanos.SECOND / 30;
    private static final long FAR_INTERVAL = Nanos.SECOND / 15;

    private static final Set<String> TICK_LOCKED = ConcurrentHashMap.newKeySet();
    private static final ClassValue<Boolean> LOCKED = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            for (Class<?> c = type; c != null; c = c.getSuperclass())
                if (TICK_LOCKED.contains(c.getName()))
                    return true;
            return false;
        }
    };

    static {
        tickLocked("de.maxhenkel.easyvillagers.blocks.tileentity.FakeWorldTileentity");
    }

    private BlockEntityReplay() {}

    // output changes per tick
    public static void tickLocked(String className) {
        TICK_LOCKED.add(className);
    }

    public static void render(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource source) {
        ExtendedBlockEntity extended = (ExtendedBlockEntity) blockEntity;
        if (!Initializer.CONFIG.blockEntityReplay || !OutputReplay.usable(blockEntity.getClass(), source, poseStack)) {
            extended.nyalkan$setReplay(null);
            Matrix4f live = poseStack.last().pose();
            ReplayTrace.count(blockEntity.getClass(), "live", live.m30(), live.m31(), live.m32());
            ReplayTrace.drawn(blockEntity, live.m30(), live.m31(), live.m32());
            dispatcher.render(blockEntity, partialTick, poseStack, source);
            return;
        }

        Matrix4f pose = poseStack.last().pose();
        float x = pose.m30(), y = pose.m31(), z = pose.m32();
        boolean locked = LOCKED.get(blockEntity.getClass());
        OutputReplay.Entry entry = OutputReplay.render(extended.nyalkan$replay(), blockEntity.getClass(), blockEntity.getBlockState(),
                interval(x, y, z), x, y, z, source, !locked, recorder -> dispatcher.render(blockEntity, partialTick, poseStack, recorder));
        if (entry != null) {
            entry.bounds = bounds(dispatcher, blockEntity);
            entry.tickLocked = locked;
        }
        extended.nyalkan$setReplay(entry);
    }

    public static boolean tryReplay(BlockEntity blockEntity, float x, float y, float z, MultiBufferSource source) {
        OutputReplay.Entry entry = ((ExtendedBlockEntity) blockEntity).nyalkan$replay();
        return entry != null && Initializer.CONFIG.blockEntityReplay
                && OutputReplay.tryReplay(entry, blockEntity.getClass(), blockEntity.getBlockState(), interval(x, y, z), x, y, z, source);
    }

    // bounds from last capture
    public static AABB cachedBounds(BlockEntity blockEntity) {
        OutputReplay.Entry entry = ((ExtendedBlockEntity) blockEntity).nyalkan$replay();
        return Initializer.CONFIG.blockEntityReplay && OutputReplay.fresh(entry, FAR_INTERVAL) ? entry.bounds : null;
    }

    private static long interval(float x, float y, float z) {
        float distance = (x + 0.5f) * (x + 0.5f) + (y + 0.5f) * (y + 0.5f) + (z + 0.5f) * (z + 0.5f);
        return distance < DistanceScale.sqr(NEAR) ? NEAR_INTERVAL : distance < DistanceScale.sqr(MID) ? MID_INTERVAL : FAR_INTERVAL;
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> AABB bounds(BlockEntityRenderDispatcher dispatcher, T blockEntity) {
        BlockEntityRenderer<T> renderer = (BlockEntityRenderer<T>) dispatcher.getRenderer(blockEntity);
        return renderer != null ? renderer.getRenderBoundingBox(blockEntity) : null;
    }
}
