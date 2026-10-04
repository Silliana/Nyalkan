package net.nyalkan.render.profiling;

import com.mojang.blaze3d.systems.RenderSystem;
import net.nyalkan.vulkan.Renderer;

import java.util.Arrays;

// profiler sections as labels
public final class DebugLabels {
    private static int[] epochs = new int[64];
    private static int depth;

    private DebugLabels() {}

    public static void push(String name) {
        if (!RenderSystem.isOnRenderThread())
            return;

        if (depth == epochs.length)
            epochs = Arrays.copyOf(epochs, depth * 2);

        epochs[depth++] = Renderer.pushDebugSection(name) ? Renderer.getLabelEpoch() : -1;
    }

    public static void pop() {
        if (!RenderSystem.isOnRenderThread() || depth == 0)
            return;

        int epoch = epochs[--depth];
        if (epoch != -1 && epoch == Renderer.getLabelEpoch())
            Renderer.popDebugSection();
    }
}
