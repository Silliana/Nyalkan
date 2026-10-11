package net.nyalkan.render.compat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.nyalkan.vulkan.Renderer;

import java.lang.reflect.Field;

public final class SkyTargetGate {
    private static final String CLIENT_INFO = "com.hollingsworth.arsnouveau.client.ClientInfo";
    private static final int KEEP_FRAMES = 30;

    private static Field target;
    private static boolean failed;
    private static int watchedId = -1;
    private static long lastUse = Long.MIN_VALUE / 2;

    private SkyTargetGate() {}

    public static void noteTexture(int id) {
        if (id == watchedId)
            lastUse = Renderer.getFrameCount();
    }

    public static boolean shouldRender() {
        RenderTarget current = current();
        if (current == null)
            return true;
        watchedId = current.getColorTextureId();
        Window window = Minecraft.getInstance().getWindow();
        if (current.width != window.getWidth() || current.height != window.getHeight())
            return true;
        return Renderer.getFrameCount() - lastUse <= KEEP_FRAMES;
    }

    private static RenderTarget current() {
        if (failed)
            return null;
        try {
            if (target == null)
                target = Class.forName(CLIENT_INFO).getField("skyRenderTarget");
            return (RenderTarget) target.get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            failed = true;
            watchedId = -1;
            return null;
        }
    }
}
