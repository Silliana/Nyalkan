package net.nyalkan.render.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.phys.Vec3;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedParticle;
import net.nyalkan.render.PipelineManager;
import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.VRenderSystem;
import net.nyalkan.vulkan.memory.VertexBuffer;
import net.nyalkan.vulkan.shader.GraphicsPipeline;
import net.nyalkan.vulkan.texture.VTextureSelector;
import net.nyalkan.vulkan.util.MappedBuffer;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Queue;

import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;

public final class GpuParticles {
    private static final int STRIDE = 64;

    public static final MappedBuffer RIGHT = new MappedBuffer(16);
    public static final MappedBuffer UP = new MappedBuffer(16);
    public static final MappedBuffer OFFSET = new MappedBuffer(16);

    private static final IdentityHashMap<ParticleRenderType, Batch> batches = new IdentityHashMap<>();
    private static int generation;
    private static VertexBuffer cornerBuffer;

    private static final class Batch {
        int generation = -1;
        int size = -1;
        ByteBuffer data = MemoryUtil.memAlloc(STRIDE * 256);
        int count;
        double originX, originY, originZ;
        final ArrayList<Particle> rest = new ArrayList<>();
    }

    private GpuParticles() {}

    public static boolean handles(ParticleRenderType type) {
        return Initializer.CONFIG.gpuParticles
                && (type == ParticleRenderType.TERRAIN_SHEET
                || type == ParticleRenderType.PARTICLE_SHEET_OPAQUE
                || type == ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT
                || type == ParticleRenderType.PARTICLE_SHEET_LIT);
    }

    public static void invalidate() {
        generation++;
    }

    public static void clear() {
        generation++;
        batches.values().forEach(batch -> batch.rest.clear());
    }

    public static Iterator<Particle> render(ParticleRenderType type, Queue<Particle> queue, Camera camera, float partialTick) {
        Batch batch = batches.computeIfAbsent(type, t -> new Batch());
        if (batch.generation != generation || batch.size != queue.size())
            build(batch, queue, camera.getPosition());

        if (batch.count > 0)
            draw(batch, camera, partialTick);

        return batch.rest.isEmpty() ? Collections.emptyIterator() : batch.rest.iterator();
    }

    private static void build(Batch batch, Queue<Particle> queue, Vec3 origin) {
        batch.generation = generation;
        batch.size = queue.size();
        batch.count = 0;
        batch.rest.clear();
        batch.originX = origin.x;
        batch.originY = origin.y;
        batch.originZ = origin.z;

        if (batch.data.capacity() < queue.size() * STRIDE)
            batch.data = MemoryUtil.memRealloc(batch.data, Math.max(queue.size(), batch.data.capacity() / STRIDE * 2) * STRIDE);

        long ptr = MemoryUtil.memAddress0(batch.data);
        for (Particle particle : queue) {
            if (particle instanceof ExtendedParticle extended && extended.nyalkan$gpuEligible()) {
                extended.nyalkan$writeInstance(ptr + (long) batch.count * STRIDE, origin.x, origin.y, origin.z);
                batch.count++;
            } else {
                batch.rest.add(particle);
            }
        }
    }

    private static void draw(Batch batch, Camera camera, float partialTick) {
        ImmediateBatcher.INSTANCE.flushGui();

        Vec3 cam = camera.getPosition();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        RIGHT.putFloat(0, -left.x());
        RIGHT.putFloat(4, -left.y());
        RIGHT.putFloat(8, -left.z());
        UP.putFloat(0, up.x());
        UP.putFloat(4, up.y());
        UP.putFloat(8, up.z());
        OFFSET.putFloat(0, (float) (batch.originX - cam.x));
        OFFSET.putFloat(4, (float) (batch.originY - cam.y));
        OFFSET.putFloat(8, (float) (batch.originZ - cam.z));
        OFFSET.putFloat(12, partialTick);

        GraphicsPipeline pipeline = PipelineManager.particleInstancedPipeline;
        Renderer renderer = Renderer.getInstance();
        VRenderSystem.setPrimitiveTopologyGL(GL11.GL_TRIANGLES);
        renderer.bindGraphicsPipeline(pipeline);
        VTextureSelector.bindShaderTextures(pipeline);
        renderer.uploadAndBindUBOs(pipeline);

        VertexBuffer instances = Renderer.getDrawer().getVertexBuffer();
        instances.copyToVertexBuffer(STRIDE, batch.count, batch.data.position(0));
        VertexBuffer corners = getCornerBuffer();

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        Renderer.getDrawer().invalidateBindings();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            vkCmdBindVertexBuffers(commandBuffer, 0,
                    stack.longs(corners.getId(), instances.getId()),
                    stack.longs(corners.getOffset(), instances.getOffset()));
        }
        vkCmdDraw(commandBuffer, 6, batch.count, 0, 0);
    }

    private static VertexBuffer getCornerBuffer() {
        if (cornerBuffer == null) {
            float[] corners = {1, -1, 1, 1, -1, 1, -1, 1, -1, -1, 1, -1};
            ByteBuffer data = MemoryUtil.memAlloc(corners.length * 4);
            data.asFloatBuffer().put(corners);
            cornerBuffer = new VertexBuffer(data.capacity());
            cornerBuffer.copyToVertexBuffer(8, 6, data);
            MemoryUtil.memFree(data);
        }
        return cornerBuffer;
    }
}
