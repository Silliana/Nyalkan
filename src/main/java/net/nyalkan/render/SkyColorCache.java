package net.nyalkan.render;

import net.nyalkan.render.Nanos;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

public final class SkyColorCache {
    private static final long MAX_AGE = 500_000_000L;

    private static Object level;
    private static Vec3 position;
    private static Vec3 color;
    private static long sampledAt;
    private static long hits, misses;
    private static String status = "idle";
    private static long statusAt;

    private SkyColorCache() {}

    public static Vec3 sample(Object owner, Vec3 at, Supplier<Vec3> sampler) {
        long now = System.nanoTime();
        if (owner == level && at.equals(position) && now - sampledAt < MAX_AGE) {
            hits++;
            updateStatus(now);
            return color;
        }
        misses++;
        updateStatus(now);
        color = sampler.get();
        level = owner;
        position = at;
        sampledAt = now;
        return color;
    }

    private static void updateStatus(long now) {
        if (now - statusAt < Nanos.SECOND)
            return;
        long total = hits + misses;
        status = total == 0 ? "idle" : (hits * 100 / total) + "% reused of " + total;
        hits = misses = 0;
        statusAt = now;
    }

    public static void addDebugLines(List<String> lines) {
        lines.add("Sky colour: " + status);
    }
}
