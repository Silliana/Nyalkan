package net.nyalkan.render.gui;

import com.mojang.blaze3d.pipeline.TextureTarget;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

final class HudBuffers {
    @SuppressWarnings("unchecked")
    private final List<TextureTarget>[] buffers = new List[]{new ArrayList<>(), new ArrayList<>()};
    private int front;
    private long cleared;
    private int width;
    private int height;

    // true if targets dropped
    boolean resize(int width, int height) {
        if (this.width == width && this.height == height)
            return false;

        for (List<TextureTarget> buffer : this.buffers) {
            for (TextureTarget target : buffer)
                target.destroyBuffers();
            buffer.clear();
        }
        this.cleared = 0;
        this.width = width;
        this.height = height;
        return true;
    }

    TextureTarget back(int segment) {
        List<TextureTarget> back = this.buffers[this.front ^ 1];
        while (back.size() <= segment) {
            TextureTarget target = new TextureTarget(this.width, this.height, true, Minecraft.ON_OSX);
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            back.add(target);
        }
        return back.get(segment);
    }

    TextureTarget front(int segment) {
        return this.buffers[this.front].get(segment);
    }

    // first capture this cycle
    boolean clearOnce(int segment) {
        if ((this.cleared & 1L << segment) != 0)
            return false;
        this.cleared |= 1L << segment;
        return true;
    }

    void startCycle() {
        this.cleared = 0;
    }

    void swap() {
        this.front ^= 1;
    }

    int count() {
        return this.buffers[0].size() + this.buffers[1].size();
    }

    // colour plus depth
    long megabytes() {
        return (long) this.count() * this.width * this.height * 8 / (1024 * 1024);
    }
}
