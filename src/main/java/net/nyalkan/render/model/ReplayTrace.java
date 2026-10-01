package net.nyalkan.render.model;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nyalkan.Initializer;
import net.nyalkan.render.Nanos;
import net.nyalkan.vulkan.Renderer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.WeakHashMap;

final class ReplayTrace {
    static final boolean ENABLED = Files.exists(Path.of("nyalkan-replay-trace"));
    private static final float RADIUS_SQR = 64.0f;
    private static final long PERIOD = 2 * Nanos.SECOND;
    private static final Object2IntOpenHashMap<String> COUNTS = new Object2IntOpenHashMap<>();
    private static final Map<BlockEntity, Long> DRAWN = new WeakHashMap<>();
    private static long loggedAt;

    private ReplayTrace() {}

    static void count(Class<?> owner, String kind, float ax, float ay, float az) {
        if (!ENABLED || ax * ax + ay * ay + az * az > RADIUS_SQR)
            return;
        COUNTS.addTo(owner.getName() + " " + kind, 1);
        long now = System.nanoTime();
        if (now - loggedAt < PERIOD)
            return;
        loggedAt = now;
        Initializer.LOGGER.info("[replayTrace] {}", COUNTS);
        COUNTS.clear();
    }

    // same block entity twice
    static void drawn(BlockEntity blockEntity, float ax, float ay, float az) {
        if (ENABLED && DRAWN.put(blockEntity, Renderer.getFrameCount()) instanceof Long frame && frame == Renderer.getFrameCount())
            count(blockEntity.getClass(), "dup", ax, ay, az);
    }
}
