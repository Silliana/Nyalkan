package net.nyalkan.render.gui;

import net.minecraft.client.Minecraft;
import net.nyalkan.mixin.render.batching.DeltaTrackerTimerAccessor;

public final class HudClock {
    public static final long NONE = Long.MIN_VALUE;

    private static final float NANOS_PER_TICK = 50_000_000.0F;
    private static final long MAX_GAP = 250_000_000L;
    private static final float MAX_TICKS = 7.0F;

    private HudClock() {}

    public static long begin(long lastDraw, long now) {
        if (lastDraw == 0 || now - lastDraw > MAX_GAP || !(Minecraft.getInstance().getTimer() instanceof DeltaTrackerTimerAccessor timer))
            return NONE;

        float realtime = timer.nyalkan$getRealtimeDeltaTicks();
        float game = timer.nyalkan$getDeltaTicks();
        float elapsed = Math.min((now - lastDraw) / NANOS_PER_TICK, MAX_TICKS);
        timer.nyalkan$setRealtimeDeltaTicks(elapsed);
        timer.nyalkan$setDeltaTicks(realtime > 0 ? game * elapsed / realtime : game);
        return (long) Float.floatToRawIntBits(realtime) << 32 | Float.floatToRawIntBits(game) & 0xFFFFFFFFL;
    }

    public static void end(long saved) {
        if (saved == NONE || !(Minecraft.getInstance().getTimer() instanceof DeltaTrackerTimerAccessor timer))
            return;

        timer.nyalkan$setRealtimeDeltaTicks(Float.intBitsToFloat((int) (saved >>> 32)));
        timer.nyalkan$setDeltaTicks(Float.intBitsToFloat((int) saved));
    }
}
