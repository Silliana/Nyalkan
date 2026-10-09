package net.nyalkan.render.chunk.build;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.nyalkan.render.chunk.WorldRenderer;

import java.util.function.IntSupplier;

// light reused within tick
public final class BlockEntityLightCache {
    private static final int MAX_ENTRIES = 16384;
    // packed light never negative
    private static final int MISSING = -1;
    private static final Long2IntOpenHashMap LIGHT = new Long2IntOpenHashMap();

    private static ClientLevel level;
    private static long gameTime = Long.MIN_VALUE;
    private static long blockEpoch = -1;

    static {
        LIGHT.defaultReturnValue(MISSING);
    }

    private BlockEntityLightCache() {}

    public static int get(ClientLevel current, BlockPos pos, IntSupplier lookup) {
        long time = current.getGameTime();
        long epoch = WorldRenderer.getBlockEpoch();
        if (current != level || time != gameTime || epoch != blockEpoch || LIGHT.size() > MAX_ENTRIES) {
            LIGHT.clear();
            level = current;
            gameTime = time;
            blockEpoch = epoch;
        }
        long key = pos.asLong();
        int light = LIGHT.get(key);
        if (light == MISSING) {
            light = lookup.getAsInt();
            LIGHT.put(key, light);
        }
        return light;
    }
}
