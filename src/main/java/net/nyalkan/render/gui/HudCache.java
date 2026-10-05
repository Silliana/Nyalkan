package net.nyalkan.render.gui;

import net.nyalkan.render.Nanos;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Monitor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.nyalkan.Initializer;
import net.nyalkan.gl.GlFramebuffer;
import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.VRenderSystem;
import net.nyalkan.vulkan.shader.PipelineState;
import net.nyalkan.vulkan.texture.VTextureSelector;
import net.nyalkan.vulkan.texture.VulkanImage;
import net.nyalkan.tick.FrameBudget;
import net.nyalkan.vulkan.util.DrawUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;

public final class HudCache {
    public enum Mode { LIVE, ACTIVE }

    private static final long SECOND = Nanos.SECOND;
    private static final int MAX_PASSES = 4;
    private static final int MAX_HELD_FRAMES = 2;
    private static final boolean DEBUG = Boolean.getBoolean("nyalkan.hudCacheDebug");
    private static final Set<String> REPORTED = new HashSet<>();

    private static int refreshRate;
    private static long refreshCheckedAt;
    private static HudCache capturing;
    private static final List<HudCache> ALL = new CopyOnWriteArrayList<>();
    private volatile String status = "idle";
    private int fixedHz;
    private boolean forceLive;
    private HudCache outer;
    private int outerBlend;

    private final String name;
    private static final int MAX_SEGMENTS = Long.SIZE;
    private final PipelineState.BlendInfo savedBlend = PipelineState.defaultBlendInfo();
    private final HudBuffers targets = new HudBuffers();
    private final HudLayerCosts costs = new HudLayerCosts();
    private ClientLevel level;
    private int structure;
    private Mode mode = Mode.LIVE;

    private long lastCallFrame = -1;
    private long lastDraw;
    private long resizedAt = Long.MIN_VALUE / 2;
    private long lastCallTime;
    private long lastFrameTime;
    private double avgFrameTime;

    private boolean valid;
    private int validSegments;

    private boolean cycleActive;
    private long cycleStart;
    private int passes = 1;
    private int heldFrames;
    private int pass;
    private int[] bounds = {0, Integer.MAX_VALUE};
    private long frameSegments;
    private int cycleSegments;

    private int layerIndex;
    private int layerCount;

    private int segment = -1;
    private boolean sideBuffer;
    private boolean clearedColor;

    private int cycles, frames;
    private long debugAt;

    public HudCache(String name) {
        this(name, 0);
    }

    // fixed rate ignores monitor
    public HudCache(String name, int fixedHz) {
        this.name = name;
        this.fixedHz = fixedHz;
        ALL.add(this);
    }

    public static void addDebugLines(List<String> lines) {
        for (HudCache cache : ALL)
            lines.add("HUD cache " + cache.name + ": " + cache.status + ", " + cache.targets.count() + " targets ~" + cache.targets.megabytes() + "MB");
    }

    // fixed caches only
    public void setRate(int hz) {
        if (this.fixedHz > 0 && hz > 0)
            this.fixedHz = hz;
    }

    public static boolean isCapturing() {
        return capturing != null;
    }

    public void render(GuiGraphics graphics, Runnable draw) {
        if (isCapturing() && this.fixedHz == 0 || this.forceLive || this.begin(0) == Mode.LIVE) {
            this.lastDraw = System.nanoTime();
            draw.run();
            return;
        }

        if (this.nextLayer()) {
            this.capture(graphics, 0);
            long now = System.nanoTime();
            long clock = HudClock.begin(this.lastDraw, now);
            this.lastDraw = now;
            boolean bad;
            try {
                draw.run();
            } finally {
                HudClock.end(clock);
                graphics.flush();
                ImmediateBatcher.INSTANCE.flush();
                bad = this.checkLayer(this.name);
                this.endCapture(graphics);
            }
            if (bad) {
                this.forceLive = true;
                this.status = "live (uncacheable drawing)";
                draw.run();
                return;
            }
        }
        this.composite(0);
        this.finish();
    }

    public Mode begin(int structure) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget main = minecraft.getMainRenderTarget();
        long now = System.nanoTime();
        long frame = Renderer.getFrameCount();

        int hz = this.fixedHz > 0 ? this.fixedHz : refreshRate(now);
        long interval = hz > 0 ? SECOND / hz : 0;
        boolean nested = isCapturing() && this.lastCallFrame != frame && now - this.lastCallTime < interval;
        if (this.lastCallTime != 0 && (this.lastCallFrame == frame - 1 || nested)) {
            long frameTime = now - this.lastCallTime;
            this.lastFrameTime = frameTime;
            this.avgFrameTime = this.avgFrameTime == 0 ? frameTime : this.avgFrameTime * 0.9 + frameTime * 0.1;
        } else {
            this.invalidate();
        }
        this.lastCallFrame = frame;
        this.lastCallTime = now;
        this.layerIndex = 0;
        this.frameSegments = 0;

        double limit = this.mode == Mode.ACTIVE ? interval : interval * 0.85;
        int low = this.fixedHz > 0 ? 0 : Initializer.CONFIG.hudLowFpsRate;
        if (low > 0 && low < hz && interval > 0 && this.avgFrameTime >= limit) {
            hz = low;
            interval = SECOND / hz;
            limit = this.mode == Mode.ACTIVE ? interval : interval * 0.85;
        }
        String reason = this.liveReason(interval, limit);
        if (reason != null) {
            this.invalidate();
            this.status = "live (" + reason + ")";
            return this.mode = Mode.LIVE;
        }
        if (this.targets.resize(main.width, main.height)) {
            this.resizedAt = frame;
            this.invalidate();
        }
        if (frame - this.resizedAt <= Renderer.getFramesNum() + 1) {
            this.status = "live (resizing)";
            return this.mode = Mode.LIVE;
        }
        this.status = "cached at " + hz + " Hz";

        if (this.level != minecraft.level || this.structure != structure) {
            this.level = minecraft.level;
            this.structure = structure;
            this.invalidate();
        }

        if (!this.cycleActive && (!this.valid || now - this.cycleStart + this.lastFrameTime >= interval)
                && (!this.valid || !FrameBudget.heavy() || ++this.heldFrames > MAX_HELD_FRAMES)) {
            this.heldFrames = 0;
            int passes = this.valid && Initializer.CONFIG.smoothHudUpdates && minecraft.screen == null ? (int) Math.max(1, Math.min(MAX_PASSES, interval / Math.max(1, this.lastFrameTime))) : 1;
            this.startCycle(passes, now);
        }
        return this.mode = Mode.ACTIVE;
    }

    private String liveReason(long interval, double limit) {
        if (!Initializer.CONFIG.hudCaching)
            return "off";
        if (isCapturing() && this.fixedHz == 0)
            return "nested";
        if (interval == 0 || this.avgFrameTime == 0)
            return "measuring";
        if (this.avgFrameTime >= limit)
            return "fps below " + Math.round(SECOND / limit);
        return null;
    }

    public boolean nextLayer() {
        int index = this.layerIndex++;
        return this.cycleActive && index >= this.bounds[this.pass] && index < this.bounds[this.pass + 1];
    }

    public int currentLayer() {
        return this.layerIndex - 1;
    }

    public void recordCost(int layer, long nanos) {
        this.costs.record(layer, nanos);
    }

    public void capture(GuiGraphics graphics, int segment) {
        if (this.mode != Mode.ACTIVE || !this.cycleActive || capturing == this || isCapturing() && this.fixedHz == 0 || segment >= MAX_SEGMENTS)
            return;

        graphics.flush();
        ImmediateBatcher.INSTANCE.flush();

        this.segment = segment;
        TextureTarget target = this.targets.back(segment);
        this.sideBuffer = Renderer.getInstance().beginHudRecording();
        target.bindWrite(true);
        if (this.targets.clearOnce(segment))
            target.clear(Minecraft.ON_OSX);
        this.frameSegments |= 1L << segment;
        this.cycleSegments = Math.max(this.cycleSegments, segment + 1);

        this.outer = capturing;
        this.outerBlend = PipelineState.BlendState.unsupportedBlend;
        PipelineState.BlendState.coverageAlpha = true;
        PipelineState.BlendState.unsupportedBlend = -1;
        this.clearedColor = false;
        VRenderSystem.clearFilter = this::filterClear;
        capturing = this;
    }

    public boolean checkLayer(Object layer) {
        if (capturing != this)
            return false;

        boolean bad = false;
        int blend = PipelineState.BlendState.unsupportedBlend;
        if (blend != -1) {
            PipelineState.BlendState.unsupportedBlend = -1;
            bad = true;
            if (REPORTED.add(layer + " blend"))
                Initializer.LOGGER.warn("HUD cache {}: layer {} uses uncacheable blend {}/{}, drawing live", this.name, layer, blend >> 8, blend & 0xFF);
        }
        if (this.clearedColor) {
            this.clearedColor = false;
            bad = true;
            if (REPORTED.add(layer + " clear"))
                Initializer.LOGGER.warn("HUD cache {}: layer {} clears the colour buffer, drawing it live", this.name, layer);
        }
        return bad;
    }

    private int filterClear(int mask) {
        TextureTarget target = this.targets.back(this.segment);
        if ((mask & 0x4000) != 0 && GlFramebuffer.getBoundFramebuffer() == GlFramebuffer.getFramebuffer(target.frameBufferId)) {
            this.clearedColor = true;
            return mask & ~0x4000;
        }
        return mask;
    }

    public void endCapture(GuiGraphics graphics) {
        if (capturing != this)
            return;

        try {
            graphics.flush();
            ImmediateBatcher.INSTANCE.flush();
        } finally {
            HudCache outer = this.outer;
            this.outer = null;
            PipelineState.BlendState.coverageAlpha = outer != null;
            VRenderSystem.clearFilter = outer != null ? outer::filterClear : null;
            capturing = outer;
            if (outer != null)
                PipelineState.BlendState.unsupportedBlend = this.outerBlend;
            if (this.sideBuffer) {
                TextureTarget target = this.targets.back(this.segment);
                Renderer.getInstance().endHudRecording(new VulkanImage[]{GlFramebuffer.getFramebuffer(target.frameBufferId).getFramebuffer().getColorAttachment()});
            } else if (outer != null) {
                outer.targets.back(outer.segment).bindWrite(true);
            } else {
                Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            }
            this.sideBuffer = false;
            this.segment = -1;
        }
    }

    public void composite(int segment) {
        if (this.mode != Mode.ACTIVE)
            return;

        if (this.valid) {
            if (segment < this.validSegments)
                this.blit(this.targets.front(segment));
        } else if (this.passes == 1 && (this.frameSegments & 1L << segment) != 0) {
            this.blit(this.targets.back(segment));
        }
    }

    public void finish() {
        if (this.mode != Mode.ACTIVE)
            return;

        this.layerCount = this.layerIndex;
        this.frames++;
        if (this.cycleActive && ++this.pass >= this.passes) {
            this.targets.swap();
            this.validSegments = this.cycleSegments;
            this.valid = true;
            this.cycleActive = false;
            this.cycles++;
        }

        if (DEBUG)
            this.logStats();
    }

    public void invalidate() {
        this.valid = false;
        this.cycleActive = false;
    }

    private void startCycle(int passes, long now) {
        this.cycleActive = true;
        this.cycleStart = now;
        this.pass = 0;
        this.targets.startCycle();
        this.cycleSegments = 0;
        this.passes = Math.min(passes, Math.max(1, this.layerCount));
        this.bounds = this.costs.passBounds(this.layerCount, this.passes);
    }

    private void blit(TextureTarget target) {
        PipelineState.BlendInfo blend = PipelineState.blendInfo;
        this.savedBlend.copyFrom(blend);
        boolean depthTest = VRenderSystem.depthTest;
        boolean depthMask = VRenderSystem.depthMask;
        int colorMask = VRenderSystem.colorMask;

        VTextureSelector.bindTexture(0, GlFramebuffer.getFramebuffer(target.frameBufferId).getFramebuffer().getColorAttachment());
        VRenderSystem.colorMask(true, true, true, isCapturing());
        VRenderSystem.disableDepthTest();
        VRenderSystem.depthMask(false);
        blend.enabled = true;
        blend.srcRgbFactor = VK_BLEND_FACTOR_ONE;
        blend.dstRgbFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
        blend.srcAlphaFactor = VK_BLEND_FACTOR_ONE;
        blend.dstAlphaFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;

        DrawUtil.blitToScreen(false);

        blend.copyFrom(this.savedBlend);
        VRenderSystem.depthTest = depthTest;
        VRenderSystem.depthMask = depthMask;
        VRenderSystem.colorMask = colorMask;
    }

    private void logStats() {
        long now = System.nanoTime();
        if (this.debugAt == 0)
            this.debugAt = now;
        if (now - this.debugAt < 5 * SECOND)
            return;

        Initializer.LOGGER.info("HUD cache {}: {} full updates over {} frames in 5s, {} passes, {} layers, {} segments ({} Hz)",
                this.name, this.cycles, this.frames, this.passes, this.layerCount, this.validSegments, refreshRate);
        this.cycles = 0;
        this.frames = 0;
        this.debugAt = now;
    }

    private static int refreshRate(long now) {
        if (now - refreshCheckedAt >= SECOND || refreshCheckedAt == 0) {
            refreshCheckedAt = now;
            try {
                Monitor monitor = Minecraft.getInstance().getWindow().findBestMonitor();
                refreshRate = monitor != null ? monitor.getCurrentMode().getRefreshRate() : 0;
            } catch (RuntimeException e) {
                refreshRate = 0;
            }
        }
        return refreshRate;
    }
}
