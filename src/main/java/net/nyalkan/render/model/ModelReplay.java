package net.nyalkan.render.model;

import net.nyalkan.render.vertex.EntityVertex;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.nyalkan.interfaces.ExtendedModelPart;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.util.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public final class ModelReplay {
    private static final int STRIDE = EntityVertex.STRIDE;

    public static long bulkVertices;

    private final Slot[] slots = {new Slot(), new Slot()};
    private int lastUsed;
    private float[] scratchState = new float[0];
    private ModelPart sizedRoot;
    private int stateSize;

    private static final class Slot {
        final Matrix4f pose = new Matrix4f();
        final Matrix3f normal = new Matrix3f();
        float[] state = new float[0];
        ByteBuffer vertices;
        long frame = -1;
        int vertexCount;
        int light, overlay, color;
    }

    public boolean tryReplay(ModelPart root, PoseStack.Pose pose, ExtendedVertexBuilder builder, int light, int overlay, int color) {
        long frame = Renderer.getFrameCount();
        boolean captured = false;

        for (int i = 0; i < this.slots.length; i++) {
            Slot slot = this.slots[i];
            if (slot.frame != frame || slot.vertexCount == 0)
                continue;
            if (!slot.pose.equals(pose.pose()) || !slot.normal.equals(pose.normal()))
                continue;

            if (!captured) {
                this.scratchState = this.captureState(root, this.scratchState);
                captured = true;
            }
            if (!Arrays.equals(slot.state, this.scratchState))
                continue;

            this.lastUsed = i;
            copy(slot, builder, light, overlay, color);
            return true;
        }
        return false;
    }

    public void record(ModelPart root, PoseStack.Pose pose, long start, int vertexCount, int light, int overlay, int color) {
        int index = this.lastUsed ^ 1;
        Slot slot = this.slots[index];
        this.lastUsed = index;

        int bytes = vertexCount * STRIDE;
        if (slot.vertices == null || slot.vertices.capacity() < bytes)
            slot.vertices = ByteBuffer.allocateDirect(Math.max(bytes, 4096));

        MemoryUtil.memCopy(start, MemoryUtil.memAddress(slot.vertices), bytes);
        slot.pose.set(pose.pose());
        slot.normal.set(pose.normal());
        slot.state = this.captureState(root, slot.state);
        slot.frame = Renderer.getFrameCount();
        slot.vertexCount = vertexCount;
        slot.light = light;
        slot.overlay = overlay;
        slot.color = color;
    }

    public void invalidate() {
        for (Slot slot : this.slots)
            slot.vertexCount = 0;
    }

    private static void copy(Slot slot, ExtendedVertexBuilder builder, int light, int overlay, int color) {
        long dst = builder.reserveEntityVertices(slot.vertexCount);
        MemoryUtil.memCopy(MemoryUtil.memAddress(slot.vertices), dst, (long) slot.vertexCount * STRIDE);

        if (light != slot.light || overlay != slot.overlay || color != slot.color) {
            int rgba = ColorUtil.RGBA.fromArgb32(color);
            for (int i = 0; i < slot.vertexCount; i++) {
                long ptr = dst + (long) i * STRIDE;
                MemoryUtil.memPutInt(ptr + EntityVertex.COLOR, rgba);
                MemoryUtil.memPutInt(ptr + EntityVertex.OVERLAY, overlay);
                MemoryUtil.memPutInt(ptr + EntityVertex.LIGHT, light);
            }
        }
    }

    private float[] captureState(ModelPart root, float[] dst) {
        ExtendedModelPart part = (ExtendedModelPart) (Object) root;
        if (this.sizedRoot != root) {
            this.sizedRoot = root;
            this.stateSize = part.nyalkan$stateSize();
        }

        if (dst.length != this.stateSize)
            dst = new float[this.stateSize];

        try {
            if (part.nyalkan$writeState(dst, 0) == this.stateSize)
                return dst;
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }

        this.stateSize = part.nyalkan$stateSize();
        dst = new float[this.stateSize];
        part.nyalkan$writeState(dst, 0);
        return dst;
    }
}
