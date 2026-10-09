package net.nyalkan.render.immediate;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.nyalkan.Initializer;
import net.nyalkan.vulkan.VRenderSystem;
import net.nyalkan.interfaces.ExtendedRenderType;
import net.nyalkan.interfaces.ExtendedVertexBuilder;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;

public final class ImmediateBatcher {
    public static final ImmediateBatcher INSTANCE = new ImmediateBatcher();

    private record Pending(RenderType type, ByteBufferBuilder memory, BufferBuilder builder, int rank, boolean deferred) {}

    // draw order
    private final ArrayList<Pending> pending = new ArrayList<>();
    private final HashMap<RenderType, Pending> latest = new HashMap<>();
    private final ArrayDeque<ByteBufferBuilder> memoryPool = new ArrayDeque<>();
    private final StateSnapshot state = new StateSnapshot();

    private int nextRank;
    private boolean flushing;
    private int stateEpoch, textureEpoch;
    private boolean guiMode;

    private int maxRank = -1;
    private int[] savedMaxRanks = new int[8];
    private int depth;

    private ImmediateBatcher() {}


    public static boolean isEnabled() {
        return Initializer.CONFIG.immediateBatching;
    }

    public boolean isGuiMode() {
        return this.guiMode;
    }

    // false when already batching
    public boolean beginGuiScope() {
        if (!Initializer.CONFIG.hudBatching || !isEnabled() || this.guiMode)
            return false;
        this.beginGui();
        return true;
    }

    public void beginGui() {
        this.flush();
        this.guiMode = true;
    }

    public void endGui() {
        this.flush();
        this.guiMode = false;
    }

    private int suspended;

    private boolean watching, touched;

    public void watchExternal() {
        this.watching = true;
        this.touched = false;
    }

    public boolean endWatch() {
        this.watching = false;
        return this.touched;
    }

    public void touchExternal() {
        if (this.watching)
            this.touched = true;
    }

    public void beforeStateChange() {
        this.touchExternal();
        this.stateEpoch++;
        if (!this.pending.isEmpty() && !this.flushing)
            this.flush();
    }

    // callers hold buffers
    public void suspend() {
        this.flush();
        this.suspended++;
    }

    public void resume() {
        if (this.suspended > 0)
            this.suspended--;
    }

    public boolean isSuspended() {
        return this.suspended > 0;
    }

    public void textureChanged() {
        this.touchExternal();
        this.textureEpoch++;
        if (this.guiMode)
            this.flush();
    }

    private int lastDeferredRank = -1;

    public void flushGui() {
        this.touchExternal();
        if (this.guiMode)
            this.flush();
    }

    public BufferBuilder getBuffer(RenderType type, Runnable closeOpenShared) {
        return this.getBuffer(type, closeOpenShared, false);
    }

    // drawn after opaque batches
    public BufferBuilder getDeferredBuffer(RenderType type, Runnable closeOpenShared) {
        return this.getBuffer(type, closeOpenShared, true);
    }

    private BufferBuilder getBuffer(RenderType type, Runnable closeOpenShared, boolean deferred) {
        if (!this.pending.isEmpty() && !this.state.matchesCurrent())
            flush();

        Pending entry = this.latest.get(type);
        if (deferred && entry != null && (entry.rank != this.lastDeferredRank || !type.canConsolidateConsecutiveGeometry()))
            entry = null;
        else if (entry != null && !this.guiMode && entry.rank < this.maxRank)
            entry = null;
        else if (entry != null && this.guiMode && entry.rank < this.nextRank - 1 && !((ExtendedRenderType) type).nyalkan$isBatchable())
            // only opaque draws reorder
            entry = null;

        if (entry == null) {
            closeOpenShared.run();

            if (this.pending.isEmpty())
                this.state.capture();

            ByteBufferBuilder memory = this.memoryPool.isEmpty() ? new ByteBufferBuilder(type.bufferSize()) : this.memoryPool.pop();
            entry = new Pending(type, memory, new BufferBuilder(memory, type.mode(), type.format()), this.nextRank++, deferred);
            if (deferred)
                this.lastDeferredRank = entry.rank;
            ((ExtendedVertexBuilder) entry.builder).setCullsBackFaces(((ExtendedRenderType) type).nyalkan$culls());
            this.pending.add(entry);
            this.latest.put(type, entry);
        }

        if (!deferred)
            this.maxRank = Math.max(this.maxRank, entry.rank);
        return entry.builder;
    }

    public void flush() {
        this.touchExternal();
        if (this.pending.isEmpty() || this.flushing)
            return;

        this.flushing = true;
        // gui flush runs depthless
        boolean restoreDepth = this.guiMode && VRenderSystem.depthTest;
        if (restoreDepth)
            RenderSystem.disableDepthTest();
        try {
            for (Pending entry : this.pending)
                if (!entry.deferred && !((ExtendedRenderType) entry.type).nyalkan$isDecal())
                    this.draw(entry);
            for (Pending entry : this.pending)
                if (!entry.deferred && ((ExtendedRenderType) entry.type).nyalkan$isDecal())
                    this.draw(entry);
            for (Pending entry : this.pending)
                if (entry.deferred)
                    this.draw(entry);
        } finally {
            if (restoreDepth)
                RenderSystem.enableDepthTest();
            this.pending.clear();
            this.latest.clear();
            this.lastDeferredRank = -1;
            this.nextRank = 0;
            this.maxRank = -1;
            Arrays.fill(this.savedMaxRanks, 0, this.depth, -1);
            this.flushing = false;
        }
    }

    private void draw(Pending entry) {
        MeshData meshData = entry.builder.build();
        if (meshData != null) {
            if (entry.type.sortOnUpload())
                meshData.sortQuads(entry.memory, RenderSystem.getVertexSorting());
            entry.type.draw(meshData);
        }

        this.memoryPool.push(entry.memory);
    }

    public void beginEntity() {
        if (this.depth == this.savedMaxRanks.length)
            this.savedMaxRanks = Arrays.copyOf(this.savedMaxRanks, this.depth * 2);

        this.savedMaxRanks[this.depth++] = this.maxRank;
        this.maxRank = -1;
    }

    public void endEntity() {
        if (this.depth == 0)
            return;

        // keep the stricter constraint
        this.maxRank = Math.max(this.savedMaxRanks[--this.depth], this.maxRank);
    }

    private static final class StateSnapshot {
        private int stateEpoch, textureEpoch;

        void capture() {
            this.stateEpoch = INSTANCE.stateEpoch;
            this.textureEpoch = INSTANCE.textureEpoch;
        }

        boolean matchesCurrent() {
            return this.stateEpoch == INSTANCE.stateEpoch
                    && (!INSTANCE.guiMode || this.textureEpoch == INSTANCE.textureEpoch);
        }
    }
}
