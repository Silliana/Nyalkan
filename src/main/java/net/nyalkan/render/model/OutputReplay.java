package net.nyalkan.render.model;

import com.mojang.blaze3d.systems.RenderSystem;
import net.nyalkan.render.Nanos;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.tick.FrameBudget;
import org.joml.Matrix4fc;
import net.nyalkan.tick.TickSpreader;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class OutputReplay {
    private static final int MAX_STRIKES = 4;
    private static final int MAX_CHANGES = 3;
    private static final float SAME = 1.0e-4f;

    private static final ClassValue<Strikes> STRIKES = new ClassValue<>() {
        @Override
        protected Strikes computeValue(Class<?> type) {
            return new Strikes();
        }
    };

    private static final class Strikes {
        private int count;

        boolean banned() {
            return this.count >= MAX_STRIKES;
        }

        void bump() {
            this.count++;
        }

        void ban() {
            this.count = MAX_STRIKES;
        }
    }

    private static final ArrayList<RenderType> types = new ArrayList<>();
    private static final ArrayList<BufferBuilder> builders = new ArrayList<>();
    private static final ArrayList<ByteBufferBuilder> memory = new ArrayList<>();
    private static final MultiBufferSource RECORDER = OutputReplay::recordingBuffer;

    public static final class Entry {
        Object state;
        long time;
        public AABB bounds;
        float ax, ay, az;
        RenderType[] types;
        int[] counts;
        ByteBuffer data;
        int changes;
        int tick;
        public boolean tickLocked;
    }

    private OutputReplay() {}

    public static boolean usable(Class<?> owner, MultiBufferSource source, PoseStack poseStack) {
        return !STRIKES.get(owner).banned() && source == Minecraft.getInstance().renderBuffers().bufferSource()
                && (poseStack.last().pose().properties() & Matrix4fc.PROPERTY_TRANSLATION) != 0;
    }

    private static final long SOON = Nanos.SECOND / 60;
    private static final long TICK = Nanos.SECOND / 20;

    public static void refreshSoon(Entry entry) {
        long due = System.nanoTime() - SOON + (long) (ThreadLocalRandom.current().nextDouble() * SOON);
        if (entry.time > due)
            entry.time = due;
    }

    public static boolean fresh(Entry entry, long maxAge) {
        return entry != null && System.nanoTime() - entry.time < maxAge;
    }

    public static boolean tryReplay(Entry entry, Class<?> owner, Object state, long interval, float ax, float ay, float az, MultiBufferSource source) {
        if (STRIKES.get(owner).banned() || entry.state != state || !current(entry, interval) || !replay(entry, source, ax, ay, az))
            return false;
        ReplayTrace.count(owner, "replay", ax, ay, az);
        return true;
    }

    private static boolean current(Entry entry, long interval) {
        if (entry.tickLocked) {
            if (entry.tick == TickSpreader.tickIndex())
                return true;
            if (interval < TICK)
                return false;
        }
        long age = System.nanoTime() - entry.time;
        if (age < interval || age < interval * 2 && FrameBudget.heavy())
            return true;
        if (age < interval * 3)
            return false;
        long now = System.nanoTime();
        entry.time = now - interval + (long) (ThreadLocalRandom.current().nextDouble() * Math.min(interval, SOON));
        return true;
    }

    public static Entry render(Entry entry, Class<?> owner, Object state, long interval, float ax, float ay, float az,
                               MultiBufferSource source, boolean watchChanges, Consumer<MultiBufferSource> draw) {
        RenderSystem.assertOnRenderThread();
        long now = System.nanoTime();
        if (entry != null && entry.state == state && current(entry, interval) && replay(entry, source, ax, ay, az)) {
            ReplayTrace.count(owner, "replay", ax, ay, az);
            return entry;
        }
        ReplayTrace.count(owner, "capture", ax, ay, az);

        ImmediateBatcher batcher = ImmediateBatcher.INSTANCE;
        boolean fresh = entry == null;
        boolean external;
        batcher.watchExternal();
        try {
            draw.accept(RECORDER);
        } finally {
            external = batcher.endWatch();
            entry = collect(entry, watchChanges && !fresh, ax, ay, az);
        }

        entry.state = state;
        entry.ax = ax;
        entry.ay = ay;
        entry.az = az;
        entry.tick = TickSpreader.tickIndex();
        entry.time = fresh ? now - (long) (ThreadLocalRandom.current().nextDouble() * interval) : now;

        Strikes strikes = STRIKES.get(owner);
        if (!replay(entry, source, ax, ay, az)) {
            strikes.ban();
            return null;
        }
        if (external) {
            strikes.bump();
            return null;
        }
        if (entry.changes >= MAX_CHANGES) {
            strikes.ban();
            Initializer.LOGGER.info("[replay] {} output keeps changing, rendering live", owner.getName());
            return null;
        }
        return entry;
    }

    private static VertexConsumer recordingBuffer(RenderType type) {
        int index = types.indexOf(type);
        if (index >= 0)
            return builders.get(index);

        if (memory.size() == types.size())
            memory.add(new ByteBufferBuilder(type.bufferSize()));
        BufferBuilder builder = new BufferBuilder(memory.get(types.size()), type.mode(), type.format());
        types.add(type);
        builders.add(builder);
        return builder;
    }

    private static Entry collect(Entry entry, boolean compare, float ax, float ay, float az) {
        if (entry == null)
            entry = new Entry();

        int size = types.size();
        MeshData[] meshes = new MeshData[size];
        int bytes = 0;
        for (int i = 0; i < size; i++) {
            meshes[i] = builders.get(i).build();
            if (meshes[i] != null)
                bytes += meshes[i].vertexBuffer().remaining();
        }

        RenderType[] oldTypes = entry.types;
        int[] oldCounts = entry.counts;
        boolean same = compare && oldTypes != null && oldTypes.length == size && entry.data.capacity() >= bytes;
        float dx = ax - entry.ax, dy = ay - entry.ay, dz = az - entry.az;

        if (entry.data == null || entry.data.capacity() < bytes)
            entry.data = ByteBuffer.allocateDirect(Math.max(bytes, 256));
        entry.types = new RenderType[size];
        entry.counts = new int[size];
        long dst = MemoryUtil.memAddress0(entry.data);
        for (int i = 0; i < size; i++) {
            entry.types[i] = types.get(i);
            MeshData mesh = meshes[i];
            int count = mesh == null ? 0 : mesh.drawState().vertexCount();
            if (same && (oldTypes[i] != entry.types[i] || oldCounts[i] != count))
                same = false;
            if (mesh == null)
                continue;
            ByteBuffer vertices = mesh.vertexBuffer();
            int length = vertices.remaining();
            long src = MemoryUtil.memAddress(vertices);
            if (same)
                same = sameVertices(dst, src, count, entry.types[i].format().getVertexSize(), dx, dy, dz);
            MemoryUtil.memCopy(src, dst, length);
            dst += length;
            entry.counts[i] = count;
            mesh.close();
        }
        entry.changes = !compare || same ? 0 : entry.changes + 1;

        types.clear();
        builders.clear();
        return entry;
    }

    private static boolean sameVertices(long old, long now, int count, int stride, float dx, float dy, float dz) {
        for (long end = old + (long) count * stride; old < end; old += stride, now += stride) {
            if (Math.abs(MemoryUtil.memGetFloat(now) - MemoryUtil.memGetFloat(old) - dx) > SAME
                    || Math.abs(MemoryUtil.memGetFloat(now + 4) - MemoryUtil.memGetFloat(old + 4) - dy) > SAME
                    || Math.abs(MemoryUtil.memGetFloat(now + 8) - MemoryUtil.memGetFloat(old + 8) - dz) > SAME)
                return false;
            int o = 12;
            for (; o + 4 <= stride; o += 4)
                if (MemoryUtil.memGetInt(now + o) != MemoryUtil.memGetInt(old + o))
                    return false;
            for (; o < stride; o++)
                if (MemoryUtil.memGetByte(now + o) != MemoryUtil.memGetByte(old + o))
                    return false;
        }
        return true;
    }

    private static boolean replay(Entry entry, MultiBufferSource source, float ax, float ay, float az) {
        float dx = ax - entry.ax, dy = ay - entry.ay, dz = az - entry.az;
        long src = MemoryUtil.memAddress0(entry.data);
        ImmediateBatcher batcher = ImmediateBatcher.INSTANCE;
        batcher.beginEntity();
        try {
            for (int i = 0; i < entry.types.length; i++) {
                int count = entry.counts[i];
                if (count == 0)
                    continue;

                RenderType type = entry.types[i];
                VertexFormat format = type.format();
                if (format.getOffset(VertexFormatElement.POSITION) != 0 || !format.contains(VertexFormatElement.POSITION)
                        || !(source.getBuffer(type) instanceof ExtendedVertexBuilder builder) || builder.vertexFormat() != format)
                    return false;

                int stride = format.getVertexSize();
                long bytes = (long) count * stride;
                long dst = builder.reserveEntityVertices(count);
                MemoryUtil.memCopy(src, dst, bytes);
                if (dx != 0.0f || dy != 0.0f || dz != 0.0f) {
                    for (long ptr = dst, end = dst + bytes; ptr < end; ptr += stride) {
                        MemoryUtil.memPutFloat(ptr, MemoryUtil.memGetFloat(ptr) + dx);
                        MemoryUtil.memPutFloat(ptr + 4, MemoryUtil.memGetFloat(ptr + 4) + dy);
                        MemoryUtil.memPutFloat(ptr + 8, MemoryUtil.memGetFloat(ptr + 8) + dz);
                    }
                }
                src += bytes;
            }
        } finally {
            batcher.endEntity();
        }
        return true;
    }
}
