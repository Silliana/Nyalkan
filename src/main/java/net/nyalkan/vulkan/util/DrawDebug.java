package net.nyalkan.vulkan.util;

import net.minecraft.client.Minecraft;
import net.nyalkan.Initializer;
import net.nyalkan.vulkan.Renderer;

// draw count debug
public abstract class DrawDebug {
    public static final boolean ENABLED = Boolean.getBoolean("nyalkan.debugDraws");

    public static int drawWithShader, draw, emptyMesh, formatSkip, spriteUploads, spriteCopyCalls, spritesTicked, spritesSkipped;
    private static int frames;
    private static long lastLog;

    public static void frameEnd() {
        frames++;
        long now = System.currentTimeMillis();
        if (now - lastLog < 1000)
            return;
        lastLog = now;

        Minecraft mc = Minecraft.getInstance();
        Initializer.LOGGER.info("[debugDraws] frames={} drawWithShader={} draw={} emptyMesh={} formatSkip={} spriteUploads={} spriteCopyCalls={} spritesTicked={} spritesSkipped={} | noRender={} loadFinished={} overlay={} screen={} skipRendering={}",
                frames, drawWithShader, draw, emptyMesh, formatSkip, spriteUploads, spriteCopyCalls, spritesTicked, spritesSkipped,
                mc.noRender, mc.isGameLoadFinished(),
                mc.getOverlay() == null ? null : mc.getOverlay().getClass().getName(),
                mc.screen == null ? null : mc.screen.getClass().getName(),
                Renderer.skipRendering);

        frames = drawWithShader = draw = emptyMesh = formatSkip = spriteUploads = spriteCopyCalls = spritesTicked = spritesSkipped = 0;
    }
}
