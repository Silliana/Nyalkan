package net.nyalkan.vulkan.texture;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.nyalkan.render.texture.SpriteUtil;
import net.nyalkan.vulkan.device.DeviceManager;
import net.nyalkan.vulkan.util.DrawDebug;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;

import static org.lwjgl.vulkan.VK10.*;

// one copy per atlas
public final class SpriteUploadBatch {
    private static final int REGION_INTS = 7;

    private static final IdentityHashMap<VulkanImage, Pending> pending = new IdentityHashMap<>();
    private static final ArrayList<Pending> pool = new ArrayList<>();
    private static VkBufferImageCopy.Buffer regions = VkBufferImageCopy.calloc(256);

    private static boolean tickActive;
    private static int spriteDepth;

    private static final class Pending {
        long stagingBuffer;
        int[] data = new int[REGION_INTS * 64];
        int count;
        final LongOpenHashSet origins = new LongOpenHashSet();

        void reset(long stagingBuffer) {
            this.stagingBuffer = stagingBuffer;
            this.count = 0;
            this.origins.clear();
        }
    }

    private SpriteUploadBatch() {}

    public static void beginTick() {
        tickActive = true;
    }

    public static void endTick() {
        flushAll();
        tickActive = false;
        spriteDepth = 0;
    }

    public static void beginSprite() {
        spriteDepth++;
    }

    public static void endSprite() {
        spriteDepth--;
    }

    public static boolean isActive() {
        return tickActive && spriteDepth > 0;
    }

    public static void add(VulkanImage image, long stagingBuffer, int bufferOffset, int rowLength, int mipLevel,
                           int x, int y, int width, int height) {
        Pending entry = pending.get(image);

        long origin = ((long) mipLevel << 48) | ((long) x << 24) | y;
        if (entry != null && (entry.stagingBuffer != stagingBuffer || entry.origins.contains(origin)))
            flush(image);

        entry = pending.get(image);
        if (entry == null) {
            entry = pool.isEmpty() ? new Pending() : pool.remove(pool.size() - 1);
            entry.reset(stagingBuffer);
            pending.put(image, entry);
            SpriteUtil.addTransitionedLayout(image);
        }

        int base = entry.count * REGION_INTS;
        if (base + REGION_INTS > entry.data.length)
            entry.data = Arrays.copyOf(entry.data, entry.data.length * 2);

        int[] d = entry.data;
        d[base] = bufferOffset;
        d[base + 1] = rowLength;
        d[base + 2] = mipLevel;
        d[base + 3] = x;
        d[base + 4] = y;
        d[base + 5] = width;
        d[base + 6] = height;
        entry.count++;
        entry.origins.add(origin);

        if (DrawDebug.ENABLED) DrawDebug.spriteUploads++;
    }

    public static void flush(VulkanImage image) {
        if (pending.isEmpty())
            return;

        Pending entry = pending.remove(image);
        if (entry == null)
            return;

        record(image, entry);
        pool.add(entry);
    }

    private static void flushAll() {
        if (pending.isEmpty())
            return;

        pending.forEach(SpriteUploadBatch::record);
        pool.addAll(pending.values());
        pending.clear();
    }

    private static void record(VulkanImage image, Pending entry) {
        if (entry.count == 0)
            return;

        if (regions.capacity() < entry.count) {
            regions.free();
            regions = VkBufferImageCopy.calloc(Math.max(entry.count, regions.capacity() * 2));
        }

        VkCommandBuffer commandBuffer = DeviceManager.getGraphicsQueue().getCommandBuffer().getHandle();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
        }

        int[] d = entry.data;
        for (int i = 0; i < entry.count; i++) {
            int base = i * REGION_INTS;
            VkBufferImageCopy region = regions.get(i);
            region.bufferOffset(d[base]);
            region.bufferRowLength(d[base + 1]);
            region.bufferImageHeight(d[base + 6]);
            region.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, d[base + 2], 0, 1);
            region.imageOffset().set(d[base + 3], d[base + 4], 0);
            region.imageExtent().set(d[base + 5], d[base + 6], 1);
        }

        regions.position(0).limit(entry.count);
        vkCmdCopyBufferToImage(commandBuffer, entry.stagingBuffer, image.getId(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);
        regions.clear();

        if (DrawDebug.ENABLED) DrawDebug.spriteCopyCalls++;
    }
}
