package net.nyalkan.render;

import net.nyalkan.render.chunk.WorldRenderer;

public final class DistanceScale {
    private static final float BASE_CHUNKS = 12.0f;
    private static final float MIN = 0.5f;
    private static final float MAX = 2.0f;

    private DistanceScale() {}

    // tuned at twelve chunks
    public static double sqr(double blocks) {
        WorldRenderer renderer = WorldRenderer.getInstance();
        float scale = renderer == null ? 1.0f : Math.clamp(renderer.getRenderDistance() / BASE_CHUNKS, MIN, MAX);
        double scaled = blocks * scale;
        return scaled * scaled;
    }
}
