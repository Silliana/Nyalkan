package net.nyalkan.tick;

import net.minecraft.client.Minecraft;
import net.nyalkan.Initializer;

public final class FrameBudget {
    private static final long PACKET_BUDGET = 1_500_000L;
    private static final long PACKET_BUDGET_TICK = 500_000L;
    private static final long HEAVY_PACKETS = 750_000L;
    private static final int MAX_BACKLOG_FRAMES = 4;

    private static int backlogFrames;
    private static boolean heavy;

    private FrameBudget() {}

    public static void runTasks(Minecraft minecraft, int ticks) {
        long start = System.nanoTime();
        boolean enabled = Initializer.CONFIG.tickSpreading;
        if (!enabled || minecraft.level == null || backlogFrames >= MAX_BACKLOG_FRAMES) {
            while (minecraft.pollTask());
            backlogFrames = 0;
        } else {
            long deadline = start + (ticks > 0 ? PACKET_BUDGET_TICK : PACKET_BUDGET);
            while (System.nanoTime() < deadline && minecraft.pollTask());
            backlogFrames = minecraft.getPendingTasksCount() > 0 ? backlogFrames + 1 : 0;
        }
        heavy = enabled && (ticks > 0 || System.nanoTime() - start > HEAVY_PACKETS);
    }

    public static void markHeavy() {
        heavy = Initializer.CONFIG.tickSpreading;
    }

    public static boolean heavy() {
        return heavy;
    }
}
