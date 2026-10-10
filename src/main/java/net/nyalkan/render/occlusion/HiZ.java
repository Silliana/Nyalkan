package net.nyalkan.render.occlusion;

import net.minecraft.world.phys.AABB;
import net.nyalkan.Initializer;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.Vulkan;
import net.nyalkan.vulkan.device.DeviceManager;
import net.nyalkan.vulkan.memory.MemoryManager;
import net.nyalkan.vulkan.shader.ComputePipeline;
import net.nyalkan.vulkan.texture.VulkanImage;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayDeque;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

// terrain depth occlusion tests
public final class HiZ {
    private static final int TILE = 16;
    private static final float DEPTH_BIAS = 1.0e-5f;

    private static final String SHADER = """
            #version 450
            layout(local_size_x = 16, local_size_y = 16) in;
            layout(binding = 0) uniform sampler2D depthTex;
            layout(std430, binding = 1) writeonly buffer Tiles { float tiles[]; };
            layout(push_constant) uniform Push { ivec2 size; int tilesX; };
            shared float partial[256];
            void main() {
                ivec2 p = ivec2(gl_GlobalInvocationID.xy);
                uint i = gl_LocalInvocationIndex;
                partial[i] = (p.x < size.x && p.y < size.y) ? texelFetch(depthTex, p, 0).r : 0.0;
                barrier();
                for (uint s = 128u; s > 0u; s >>= 1) {
                    if (i < s)
                        partial[i] = max(partial[i], partial[i + s]);
                    barrier();
                }
                if (i == 0u)
                    tiles[gl_WorkGroupID.y * uint(tilesX) + gl_WorkGroupID.x] = partial[0];
            }
            """;

    private static final class Slot {
        long buffer, allocation, mapped;
        int capacity;
        long descriptorSet;
        boolean pending;
        int width, height, tilesX, tilesY;
        final Matrix4f viewProj = new Matrix4f();
        double camX, camY, camZ;
    }

    private static boolean initialized, failed;
    private static ComputePipeline pipeline;
    private static long setLayout, pool, sampler;
    private static Slot[] slots;

    private static long depthImage, depthView;
    private static final ArrayDeque<long[]> retiredViews = new ArrayDeque<>();

    // result two frames old
    private static float[][] levels = new float[0][];
    private static int[] levelW = new int[0], levelH = new int[0];
    private static boolean ready;
    private static final Matrix4f viewProj = new Matrix4f();
    private static double camX, camY, camZ;
    private static float screenTilesX, screenTilesY;

    private HiZ() {}

    public static boolean enabled() {
        return Initializer.CONFIG.occlusionCulling && !failed;
    }

    // after cutout terrain
    public static void capture(Matrix4f modelView, Matrix4f projection, double cx, double cy, double cz) {
        if (!enabled())
            return;
        if (!initialized && !init())
            return;

        Renderer renderer = Renderer.getInstance();
        if (renderer.getBoundFramebuffer() != Vulkan.getSwapChain())
            return;
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Slot slot = slots[Renderer.getCurrentFrame()];

        // slot fence already passed
        if (slot.pending)
            readBack(slot);

        VulkanImage depth = Vulkan.getSwapChain().getDepthAttachment();
        if (depth == null || depth.getCurrentLayout() != VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
            return;
        int width = depth.width, height = depth.height;
        int tilesX = (width + TILE - 1) / TILE, tilesY = (height + TILE - 1) / TILE;
        ensureView(depth);
        ensureBuffer(slot, tilesX * tilesY * 4);

        renderer.endRenderPass();
        try (MemoryStack stack = stackPush()) {
            VulkanImage.transitionLayout(stack, commandBuffer, depth, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                    VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                    VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT, VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT);

            writeDescriptors(stack, slot);
            pipeline.bind(commandBuffer);
            vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, pipeline.getLayout(), 0, stack.longs(slot.descriptorSet), null);
            vkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_COMPUTE_BIT, 0, stack.ints(width, height, tilesX));
            vkCmdDispatch(commandBuffer, tilesX, tilesY, 1);

            VkBufferMemoryBarrier.Buffer barrier = VkBufferMemoryBarrier.calloc(1, stack);
            barrier.sType$Default();
            barrier.srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_HOST_READ_BIT);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.buffer(slot.buffer);
            barrier.offset(0);
            barrier.size(VK_WHOLE_SIZE);
            vkCmdPipelineBarrier(commandBuffer, VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_PIPELINE_STAGE_HOST_BIT, 0, null, barrier, null);

            VulkanImage.transitionLayout(stack, commandBuffer, depth, VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL,
                    VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                    VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT, VK_ACCESS_SHADER_READ_BIT,
                    VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_LATE_FRAGMENT_TESTS_BIT,
                    VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);
        }
        renderer.getMainPass().rebindMainTarget();

        projection.mul(modelView, slot.viewProj);
        slot.camX = cx;
        slot.camY = cy;
        slot.camZ = cz;
        slot.width = width;
        slot.height = height;
        slot.tilesX = tilesX;
        slot.tilesY = tilesY;
        slot.pending = true;
        destroyRetiredViews(false);
    }

    public static boolean occluded(AABB box) {
        return occluded(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    // conservative box test
    public static boolean occluded(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!ready || !enabled())
            return false;

        float x0 = (float) (minX - camX), y0 = (float) (minY - camY), z0 = (float) (minZ - camZ);
        float x1 = (float) (maxX - camX), y1 = (float) (maxY - camY), z1 = (float) (maxZ - camZ);
        Matrix4f m = viewProj;
        float sx0 = Float.MAX_VALUE, sy0 = Float.MAX_VALUE, sx1 = -Float.MAX_VALUE, sy1 = -Float.MAX_VALUE;
        float nearest = Float.MAX_VALUE;
        for (int c = 0; c < 8; c++) {
            float x = (c & 1) == 0 ? x0 : x1, y = (c & 2) == 0 ? y0 : y1, z = (c & 4) == 0 ? z0 : z1;
            float w = m.m03() * x + m.m13() * y + m.m23() * z + m.m33();
            if (w <= 1.0e-4f)
                return false;
            float inv = 1.0f / w;
            float nx = (m.m00() * x + m.m10() * y + m.m20() * z + m.m30()) * inv;
            float ny = (m.m01() * x + m.m11() * y + m.m21() * z + m.m31()) * inv;
            float nz = (m.m02() * x + m.m12() * y + m.m22() * z + m.m32()) * inv;
            sx0 = Math.min(sx0, nx); sx1 = Math.max(sx1, nx);
            sy0 = Math.min(sy0, ny); sy1 = Math.max(sy1, ny);
            nearest = Math.min(nearest, nz);
        }
        if (nearest <= 0.0f || sx1 < -1.0f || sx0 > 1.0f || sy1 < -1.0f || sy0 > 1.0f)
            return false;

        // ndc to tile cells
        float tx0 = (Math.max(sx0, -1.0f) * 0.5f + 0.5f) * screenTilesX;
        float tx1 = (Math.min(sx1, 1.0f) * 0.5f + 0.5f) * screenTilesX;
        float ty0 = (0.5f - Math.min(sy1, 1.0f) * 0.5f) * screenTilesY;
        float ty1 = (0.5f - Math.max(sy0, -1.0f) * 0.5f) * screenTilesY;

        int level = 0;
        float span = Math.max(tx1 - tx0, ty1 - ty0);
        while (span > 2.0f && level < levels.length - 1) {
            span *= 0.5f;
            level++;
        }
        int scale = 1 << level;
        int w = levelW[level], h = levelH[level];
        int cx0 = Math.max(0, (int) (tx0 / scale)), cx1 = Math.min(w - 1, (int) (tx1 / scale));
        int cy0 = Math.max(0, (int) (ty0 / scale)), cy1 = Math.min(h - 1, (int) (ty1 / scale));
        float[] depth = levels[level];
        for (int y = cy0; y <= cy1; y++) {
            for (int x = cx0; x <= cx1; x++) {
                if (depth[y * w + x] >= nearest - DEPTH_BIAS)
                    return false;
            }
        }
        return true;
    }

    private static void readBack(Slot slot) {
        slot.pending = false;
        Vma.vmaInvalidateAllocation(Vulkan.getAllocator(), slot.allocation, 0, VK_WHOLE_SIZE);

        int w = slot.tilesX, h = slot.tilesY;
        int count = 1;
        for (int s = Math.max(w, h); s > 1; s = (s + 1) >> 1)
            count++;
        if (levels.length != count || levelW.length == 0 || levelW[0] != w || levelH[0] != h) {
            levels = new float[count][];
            levelW = new int[count];
            levelH = new int[count];
            int lw = w, lh = h;
            for (int i = 0; i < count; i++) {
                levels[i] = new float[lw * lh];
                levelW[i] = lw;
                levelH[i] = lh;
                lw = (lw + 1) >> 1;
                lh = (lh + 1) >> 1;
            }
        }

        float[] base = levels[0];
        MemoryUtil.memFloatBuffer(slot.mapped, base.length).get(base);

        // farthest per cell
        for (int l = 1; l < count; l++) {
            float[] src = levels[l - 1], dst = levels[l];
            int sw = levelW[l - 1], sh = levelH[l - 1], dw = levelW[l], dh = levelH[l];
            for (int y = 0; y < dh; y++) {
                for (int x = 0; x < dw; x++) {
                    int sx = x << 1, sy = y << 1;
                    float d = src[sy * sw + sx];
                    if (sx + 1 < sw) d = Math.max(d, src[sy * sw + sx + 1]);
                    if (sy + 1 < sh) {
                        d = Math.max(d, src[(sy + 1) * sw + sx]);
                        if (sx + 1 < sw) d = Math.max(d, src[(sy + 1) * sw + sx + 1]);
                    }
                    dst[y * dw + x] = d;
                }
            }
        }

        viewProj.set(slot.viewProj);
        camX = slot.camX;
        camY = slot.camY;
        camZ = slot.camZ;
        screenTilesX = slot.width / (float) TILE;
        screenTilesY = slot.height / (float) TILE;
        ready = true;
    }

    private static boolean init() {
        initialized = true;
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(2, stack);
            bindings.get(0).binding(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            bindings.get(1).binding(1).descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType$Default();
            layoutInfo.pBindings(bindings);
            LongBuffer pLayout = stack.mallocLong(1);
            check(vkCreateDescriptorSetLayout(DeviceManager.vkDevice, layoutInfo, null, pLayout), "descriptor layout");
            setLayout = pLayout.get(0);

            int frames = Renderer.getFramesNum();
            VkDescriptorPoolSize.Buffer sizes = VkDescriptorPoolSize.calloc(2, stack);
            sizes.get(0).type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(frames);
            sizes.get(1).type(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).descriptorCount(frames);
            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType$Default();
            poolInfo.pPoolSizes(sizes);
            poolInfo.maxSets(frames);
            LongBuffer pPool = stack.mallocLong(1);
            check(vkCreateDescriptorPool(DeviceManager.vkDevice, poolInfo, null, pPool), "descriptor pool");
            pool = pPool.get(0);

            LongBuffer layouts = stack.mallocLong(frames);
            for (int i = 0; i < frames; i++)
                layouts.put(i, setLayout);
            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType$Default();
            allocInfo.descriptorPool(pool);
            allocInfo.pSetLayouts(layouts);
            LongBuffer pSets = stack.mallocLong(frames);
            check(vkAllocateDescriptorSets(DeviceManager.vkDevice, allocInfo, pSets), "descriptor sets");

            VkSamplerCreateInfo samplerInfo = VkSamplerCreateInfo.calloc(stack);
            samplerInfo.sType$Default();
            samplerInfo.magFilter(VK_FILTER_NEAREST);
            samplerInfo.minFilter(VK_FILTER_NEAREST);
            samplerInfo.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST);
            samplerInfo.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            samplerInfo.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            samplerInfo.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE);
            LongBuffer pSampler = stack.mallocLong(1);
            check(vkCreateSampler(DeviceManager.vkDevice, samplerInfo, null, pSampler), "sampler");
            sampler = pSampler.get(0);

            pipeline = new ComputePipeline("hiz_reduce", SHADER, setLayout, 12);

            slots = new Slot[frames];
            for (int i = 0; i < frames; i++) {
                slots[i] = new Slot();
                slots[i].descriptorSet = pSets.get(i);
            }
            return true;
        } catch (RuntimeException e) {
            failed = true;
            Initializer.LOGGER.error("Occlusion culling disabled, setup failed", e);
            return false;
        }
    }

    private static void ensureView(VulkanImage depth) {
        if (depth.getId() == depthImage && depthView != 0)
            return;
        if (depthView != 0)
            retiredViews.add(new long[]{depthView, Renderer.getFrameCount()});
        depthImage = depth.getId();
        depthView = VulkanImage.createImageView(depth.getId(), depth.format, VK_IMAGE_ASPECT_DEPTH_BIT, 1);
        ready = false;
    }

    private static void destroyRetiredViews(boolean all) {
        long frame = Renderer.getFrameCount();
        while (!retiredViews.isEmpty() && (all || frame - retiredViews.peek()[1] > Renderer.getFramesNum() + 1))
            vkDestroyImageView(DeviceManager.vkDevice, retiredViews.poll()[0], null);
    }

    private static void ensureBuffer(Slot slot, int bytes) {
        if (slot.capacity >= bytes)
            return;
        if (slot.buffer != 0)
            MemoryManager.freeBuffer(slot.buffer, slot.allocation);

        int size = Math.max(bytes, 64 * 1024);
        try (MemoryStack stack = stackPush()) {
            LongBuffer pBuffer = stack.mallocLong(1);
            PointerBuffer pAllocation = stack.pointers(VK_NULL_HANDLE);
            MemoryManager.getInstance().createBuffer(size, VK_BUFFER_USAGE_STORAGE_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_CACHED_BIT, pBuffer, pAllocation);
            slot.buffer = pBuffer.get(0);
            slot.allocation = pAllocation.get(0);
            slot.mapped = MemoryManager.getInstance().Map(slot.allocation).get(0);
        }
        slot.capacity = size;
        slot.pending = false;
    }

    private static void writeDescriptors(MemoryStack stack, Slot slot) {
        VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc(1, stack);
        imageInfo.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_READ_ONLY_OPTIMAL);
        imageInfo.imageView(depthView);
        imageInfo.sampler(sampler);
        VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.calloc(1, stack);
        bufferInfo.buffer(slot.buffer);
        bufferInfo.offset(0);
        bufferInfo.range(VK_WHOLE_SIZE);

        VkWriteDescriptorSet.Buffer writes = VkWriteDescriptorSet.calloc(2, stack);
        writes.get(0).sType$Default().dstSet(slot.descriptorSet).dstBinding(0).descriptorCount(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).pImageInfo(imageInfo);
        writes.get(1).sType$Default().dstSet(slot.descriptorSet).dstBinding(1).descriptorCount(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER).pBufferInfo(bufferInfo);
        vkUpdateDescriptorSets(DeviceManager.vkDevice, writes, null);
    }

    private static void check(int result, String what) {
        if (result != VK_SUCCESS)
            throw new RuntimeException("Failed to create occlusion " + what + ": " + result);
    }
}
