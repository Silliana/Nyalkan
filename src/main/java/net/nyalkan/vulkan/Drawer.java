package net.nyalkan.vulkan;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.nyalkan.vulkan.memory.*;
import net.nyalkan.vulkan.util.VUtil;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;

import static org.lwjgl.vulkan.VK10.*;

public class Drawer {
    private static final int INITIAL_VB_SIZE = 2000000;
    private static final int INITIAL_UB_SIZE = 200000;

    private static final LongBuffer buffers = MemoryUtil.memAllocLong(1);
    private static final LongBuffer offsets = MemoryUtil.memAllocLong(1);
    private static final long pBuffers = MemoryUtil.memAddress0(buffers);
    private static final long pOffsets = MemoryUtil.memAddress0(offsets);

    private int framesNum;
    private VertexBuffer[] vertexBuffers;
    private final AutoIndexBuffer quadsIndexBuffer;
    private final AutoIndexBuffer quadsIntIndexBuffer;
    private final AutoIndexBuffer linesIndexBuffer;
    private final AutoIndexBuffer debugLineStripIndexBuffer;
    private final AutoIndexBuffer triangleFanIndexBuffer;
    private final AutoIndexBuffer triangleStripIndexBuffer;
    private UniformBuffer[] uniformBuffers;

    private int currentFrame;

    private VkCommandBuffer boundCommandBuffer;
    private long boundVertexBuffer, boundVertexOffset;
    private long boundIndexBuffer, boundIndexOffset;
    private int boundIndexType;

    public Drawer() {
        // Index buffers
        this.quadsIndexBuffer = new AutoIndexBuffer(AutoIndexBuffer.QUAD_U16_MAX_VERTEX_COUNT, AutoIndexBuffer.DrawType.QUADS);
        this.quadsIntIndexBuffer = new AutoIndexBuffer(100000, AutoIndexBuffer.DrawType.QUADS);
        this.linesIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.LINES);
        this.debugLineStripIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.DEBUG_LINE_STRIP);
        this.triangleFanIndexBuffer = new AutoIndexBuffer(1000, AutoIndexBuffer.DrawType.TRIANGLE_FAN);
        this.triangleStripIndexBuffer = new AutoIndexBuffer(10000, AutoIndexBuffer.DrawType.TRIANGLE_STRIP);
    }

    public void setCurrentFrame(int currentFrame) {
        this.currentFrame = currentFrame;
    }

    public void createResources(int framesNum) {
        this.framesNum = framesNum;

        if (this.vertexBuffers != null) {
            Arrays.stream(this.vertexBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.vertexBuffers = new VertexBuffer[framesNum];
        Arrays.setAll(this.vertexBuffers, i -> new VertexBuffer(INITIAL_VB_SIZE, MemoryTypes.HOST_MEM));

        if (this.uniformBuffers != null) {
            Arrays.stream(this.uniformBuffers).iterator().forEachRemaining(
                    Buffer::freeBuffer
            );
        }
        this.uniformBuffers = new UniformBuffer[framesNum];
        Arrays.setAll(this.uniformBuffers, i -> new UniformBuffer(INITIAL_UB_SIZE, MemoryTypes.HOST_MEM));
    }

    public VertexBuffer getVertexBuffer() {
        return this.vertexBuffers[this.currentFrame];
    }

    public void resetBuffers(int currentFrame) {
        this.vertexBuffers[currentFrame].reset();
        this.uniformBuffers[currentFrame].reset();
    }

    public void draw(ByteBuffer buffer, VertexFormat.Mode mode, VertexFormat vertexFormat, int vertexCount) {
        AutoIndexBuffer autoIndexBuffer;
        int indexCount;

        VertexBuffer vertexBuffer = this.vertexBuffers[this.currentFrame];
        int vertexSize = vertexFormat.getVertexSize();
        vertexBuffer.alignTo(vertexSize);
        vertexBuffer.copyToVertexBuffer(vertexSize, vertexCount, buffer);
        int firstVertex = (int) (vertexBuffer.getOffset() / vertexSize);

        switch (mode) {
            case QUADS -> {
                indexCount = vertexCount * 3 / 2;

                autoIndexBuffer = indexCount > AutoIndexBuffer.U16_MAX_INDEX_COUNT
                        ? this.quadsIntIndexBuffer : this.quadsIndexBuffer;
            }
            case LINES -> {
                autoIndexBuffer = this.linesIndexBuffer;
                indexCount = vertexCount * 3 / 2;
            }
            case TRIANGLE_FAN -> {
                autoIndexBuffer = this.triangleFanIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case TRIANGLE_STRIP, LINE_STRIP -> {
                autoIndexBuffer = this.triangleStripIndexBuffer;
                indexCount = (vertexCount - 2) * 3;
            }
            case DEBUG_LINE_STRIP -> {
                autoIndexBuffer = this.debugLineStripIndexBuffer;
                indexCount = (vertexCount - 1) * 2;
            }
            case TRIANGLES, DEBUG_LINES -> {
                indexCount = 0;
                autoIndexBuffer = null;
            }
            default -> throw new RuntimeException(String.format("unknown drawMode: %s", mode));
        }

        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();
        bindVertexBuffer(commandBuffer, vertexBuffer.getId(), 0);

        if (indexCount > 0) {
            autoIndexBuffer.checkCapacity(vertexCount);

            bindIndexBuffer(commandBuffer, autoIndexBuffer.getIndexBuffer());
            vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, firstVertex, 0);
        } else {
            vkCmdDraw(commandBuffer, vertexCount, 1, firstVertex, 0);
        }

    }

    public void drawIndexed(VertexBuffer vertexBuffer, IndexBuffer indexBuffer, int indexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        bindVertexBuffer(commandBuffer, vertexBuffer.getId(), vertexBuffer.getOffset());
        bindIndexBuffer(commandBuffer, indexBuffer);
        vkCmdDrawIndexed(commandBuffer, indexCount, 1, 0, 0, 0);
    }

    public void draw(VertexBuffer vertexBuffer, int vertexCount) {
        VkCommandBuffer commandBuffer = Renderer.getCommandBuffer();

        bindVertexBuffer(commandBuffer, vertexBuffer.getId(), vertexBuffer.getOffset());
        vkCmdDraw(commandBuffer, vertexCount, 1, 0, 0);
    }

    public void invalidateBindings() {
        this.boundCommandBuffer = null;
    }

    private void bindVertexBuffer(VkCommandBuffer commandBuffer, long buffer, long offset) {
        if (commandBuffer == this.boundCommandBuffer && buffer == this.boundVertexBuffer && offset == this.boundVertexOffset)
            return;

        if (commandBuffer != this.boundCommandBuffer) {
            this.boundCommandBuffer = commandBuffer;
            this.boundIndexBuffer = 0;
        }
        this.boundVertexBuffer = buffer;
        this.boundVertexOffset = offset;

        VUtil.UNSAFE.putLong(pBuffers, buffer);
        VUtil.UNSAFE.putLong(pOffsets, offset);
        nvkCmdBindVertexBuffers(commandBuffer, 0, 1, pBuffers, pOffsets);
    }

    public void bindIndexBuffer(VkCommandBuffer commandBuffer, IndexBuffer indexBuffer) {
        long id = indexBuffer.getId();
        long offset = indexBuffer.getOffset();
        int type = indexBuffer.indexType.type;
        if (commandBuffer == this.boundCommandBuffer && id == this.boundIndexBuffer && offset == this.boundIndexOffset && type == this.boundIndexType)
            return;

        if (commandBuffer != this.boundCommandBuffer) {
            this.boundCommandBuffer = commandBuffer;
            this.boundVertexBuffer = 0;
        }
        this.boundIndexBuffer = id;
        this.boundIndexOffset = offset;
        this.boundIndexType = type;

        vkCmdBindIndexBuffer(commandBuffer, id, offset, type);
    }

    public void cleanUpResources() {
        Buffer buffer;
        for (int i = 0; i < this.framesNum; ++i) {
            buffer = this.vertexBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

            buffer = this.uniformBuffers[i];
            MemoryManager.freeBuffer(buffer.getId(), buffer.getAllocation());

        }

        this.quadsIndexBuffer.freeBuffer();
        this.linesIndexBuffer.freeBuffer();
        this.triangleFanIndexBuffer.freeBuffer();
        this.debugLineStripIndexBuffer.freeBuffer();
    }

    public AutoIndexBuffer getQuadsIndexBuffer() {
        return this.quadsIndexBuffer;
    }

    public AutoIndexBuffer getLinesIndexBuffer() {
        return this.linesIndexBuffer;
    }

    public AutoIndexBuffer getTriangleFanIndexBuffer() {
        return this.triangleFanIndexBuffer;
    }

    public AutoIndexBuffer getTriangleStripIndexBuffer() {
        return this.triangleStripIndexBuffer;
    }

    public AutoIndexBuffer getDebugLineStripIndexBuffer() {
        return this.debugLineStripIndexBuffer;
    }

    public UniformBuffer getUniformBuffer() {
        return this.uniformBuffers[this.currentFrame];
    }

}
