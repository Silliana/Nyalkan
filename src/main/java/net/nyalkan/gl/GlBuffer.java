package net.nyalkan.gl;

import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import org.lwjgl.opengl.GL32;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.memory.MemoryManager;
import net.nyalkan.vulkan.texture.ImageUtil;
import net.nyalkan.vulkan.texture.VulkanImage;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.vulkan.VK10.*;

// TODO: This class is only used to emulate a CPU buffer for texture copying purposes
//  any other use is not supported
public class GlBuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlBuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlBuffer boundBuffer;

    private static GlBuffer pixelPackBufferBound;
    private static GlBuffer pixelUnpackBufferBound;

    // unsupported targets land here
    private static final GlBuffer otherBufferBound = new GlBuffer(-1);

    public static int glGenBuffers() {
        int id = ID_COUNTER;
        map.put(id, new GlBuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void glBindBuffer(int target, int buffer) {
        boundId = buffer;
        GlBuffer glBuffer = map.get(buffer);

        if (buffer > 0 && glBuffer == null)
            throw new NullPointerException("bound texture is null");

        if (glBuffer != null) {
            glBuffer.target = target;
        }

        // other targets ignored
        switch (target) {
            case GL32.GL_PIXEL_PACK_BUFFER -> pixelPackBufferBound = glBuffer;
            case GL32.GL_PIXEL_UNPACK_BUFFER -> pixelUnpackBufferBound = glBuffer;
            default -> {}
        }
    }

    public static void glBufferData(int target, ByteBuffer byteBuffer, int usage) {
        checkTarget(target);

        // TODO

        pixelUnpackBufferBound = boundBuffer;
    }

    public static void glBufferData(int target, long size, int usage) {
        GlBuffer buffer = switch (target) {
            case GL32.GL_PIXEL_PACK_BUFFER -> pixelPackBufferBound;
            case GL32.GL_PIXEL_UNPACK_BUFFER -> pixelUnpackBufferBound;
            default -> otherBufferBound;
        };

        buffer.allocate((int) size);
    }

    public static ByteBuffer glMapBuffer(int target, int access) {
        GlBuffer buffer = switch (target) {
            case GL32.GL_PIXEL_PACK_BUFFER -> pixelPackBufferBound;
            case GL32.GL_PIXEL_UNPACK_BUFFER -> pixelUnpackBufferBound;
            default -> otherBufferBound;
        };

        buffer.finishReadback();
        ByteBuffer mappedBuffer = buffer.data;
        mappedBuffer.position(0);
        return mappedBuffer;
    }

    public static boolean glUnmapBuffer(int i) {
        return true;
    }

    public static void glDeleteBuffers(IntBuffer intBuffer) {
        for (int i = intBuffer.position(); i < intBuffer.limit(); i++) {
            glDeleteBuffers(intBuffer.get(i));
        }
    }

    public static void glDeleteBuffers(int id) {
        var buffer = map.remove(id);

        if (buffer != null) {
            buffer.retireReadback();
            buffer.freeData();
        }
    }

    public static GlBuffer getPixelUnpackBufferBound() {
        return pixelUnpackBufferBound;
    }

    public static GlBuffer getPixelPackBufferBound() {
        return pixelPackBufferBound;
    }

    private static void checkTarget(int target) {
        if (target != GL32.GL_PIXEL_UNPACK_BUFFER && target != GL32.GL_PIXEL_PACK_BUFFER)
            throw new IllegalArgumentException("target %d not supported".formatted(target));
    }

    int id;
    int target;

    ByteBuffer data;
    private long[] readback;
    private static final ArrayList<long[]> RETIRED = new ArrayList<>();

    public GlBuffer(int id) {
        this.id = id;
    }

    private void allocate(int size) {
        this.retireReadback();
        if (this.data != null)
            this.freeData();

        this.data = MemoryUtil.memAlloc(size);
    }

    // copy lands after frame
    public void readImage(VulkanImage image, int offset) {
        freeRetired();
        this.retireReadback();

        int size = image.width * image.height * image.formatSize;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(0L);
            MemoryManager.getInstance().createBuffer(size, VK_BUFFER_USAGE_TRANSFER_DST_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pAllocation);

            VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
            Renderer.getInstance().endRenderPass(commandBuffer);
            int layout = image.getCurrentLayout();
            image.transitionImageLayout(stack, commandBuffer, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
            ImageUtil.copyImageToBuffer(commandBuffer, pBuffer.get(0), image.getId(), 0, image.width, image.height, 0, 0, 0, 0, 0);
            image.transitionImageLayout(stack, commandBuffer, layout);

            this.readback = new long[]{pBuffer.get(0), pAllocation.get(0), Renderer.getFrameCount(), Renderer.getCurrentFrame(), offset, size};
        }
    }

    private void finishReadback() {
        long[] readback = this.readback;
        if (readback == null || readback[2] == Renderer.getFrameCount())
            return;

        if (Renderer.getFrameCount() - readback[2] < Renderer.getFramesNum())
            Renderer.waitForFrame((int) readback[3]);
        long dst = MemoryUtil.memAddress0(this.data) + readback[4];
        int size = (int) Math.min(readback[5], this.data.capacity() - readback[4]);
        MemoryManager.MapAndCopy(readback[1], data -> MemoryUtil.memCopy(data.get(0), dst, size));
        MemoryManager.freeBuffer(readback[0], readback[1]);
        this.readback = null;
    }

    private void retireReadback() {
        if (this.readback != null)
            RETIRED.add(this.readback);
        this.readback = null;
    }

    private static void freeRetired() {
        RETIRED.removeIf(readback -> {
            if (Renderer.getFrameCount() - readback[2] < Renderer.getFramesNum())
                return false;
            MemoryManager.freeBuffer(readback[0], readback[1]);
            return true;
        });
    }

    private ByteBuffer getData() {
        return this.data;
    }

    private void freeData() {
        MemoryUtil.memFree(data);
    }

}
