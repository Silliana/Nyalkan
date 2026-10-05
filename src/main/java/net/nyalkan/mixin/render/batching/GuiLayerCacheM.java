package net.nyalkan.mixin.render.batching;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.GuiLayerManager;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.nyalkan.render.gui.HudCache;
import net.nyalkan.render.gui.HudClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

@Mixin(value = GuiLayerManager.class, remap = false)
public class GuiLayerCacheM {
    @Unique private static final String POST = "Lnet/neoforged/bus/api/IEventBus;post(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;";
    @Unique private static final Set<ResourceLocation> LIVE_LAYERS = Collections.newSetFromMap(new IdentityHashMap<>());

    static {
        LIVE_LAYERS.add(VanillaGuiLayers.CAMERA_OVERLAYS);
        LIVE_LAYERS.add(VanillaGuiLayers.CROSSHAIR);
    }
    @Unique private static final HudCache HUD = new HudCache("hud");

    @Unique private static HudCache.Mode nyalkan$mode;
    @Unique private static boolean nyalkan$inSegment;
    @Unique private static int nyalkan$segment;
    @Unique private static ResourceLocation nyalkan$layer;
    @Unique private static int nyalkan$drawnLayer = -1;
    @Unique private static long nyalkan$drawnAt;
    @Unique private static long nyalkan$clock = HudClock.NONE;
    @Unique private static long[] nyalkan$layerDrawnAt = new long[64];
    @Unique private static int nyalkan$leftAt, nyalkan$rightAt;
    @Unique private static int[] nyalkan$leftStep = new int[64], nyalkan$rightStep = new int[64];

    @WrapMethod(method = "render")
    private void nyalkan$cachedHud(GuiGraphics graphics, DeltaTracker partialTick, Operation<Void> original) {
        if (nyalkan$mode != null || HudCache.isCapturing()) {
            original.call(graphics, partialTick);
            return;
        }

        if (HUD.begin(LIVE_LAYERS.size()) == HudCache.Mode.LIVE) {
            Arrays.fill(nyalkan$layerDrawnAt, System.nanoTime());
            original.call(graphics, partialTick);
            return;
        }

        nyalkan$mode = HudCache.Mode.ACTIVE;
        nyalkan$inSegment = false;
        nyalkan$segment = 0;
        nyalkan$layer = null;
        nyalkan$drawnLayer = -1;
        try {
            original.call(graphics, partialTick);
        } finally {
            nyalkan$closeSegment(graphics);
            nyalkan$mode = null;
        }
        HUD.finish();
    }

    @WrapOperation(method = "renderInner", at = @At(value = "INVOKE", target = POST, ordinal = 0))
    private Event nyalkan$layerPre(IEventBus bus, Event event, Operation<Event> original) {
        if (nyalkan$mode == null || !(event instanceof RenderGuiLayerEvent.Pre pre))
            return original.call(bus, event);

        if (LIVE_LAYERS.contains(pre.getName())) {
            nyalkan$closeSegment(pre.getGuiGraphics());
            return original.call(bus, event);
        }

        nyalkan$layerDone();
        nyalkan$inSegment = true;
        Gui gui = Minecraft.getInstance().gui;
        if (!HUD.nextLayer()) {
            int index = HUD.currentLayer();
            if (index < nyalkan$leftStep.length) {
                gui.leftHeight += nyalkan$leftStep[index];
                gui.rightHeight += nyalkan$rightStep[index];
            }
            pre.setCanceled(true);
            return pre;
        }
        nyalkan$leftAt = gui.leftHeight;
        nyalkan$rightAt = gui.rightHeight;

        HUD.capture(pre.getGuiGraphics(), nyalkan$segment);
        nyalkan$layer = pre.getName();
        nyalkan$drawnLayer = HUD.currentLayer();
        nyalkan$drawnAt = System.nanoTime();
        int index = nyalkan$drawnLayer;
        if (index >= nyalkan$layerDrawnAt.length)
            nyalkan$layerDrawnAt = Arrays.copyOf(nyalkan$layerDrawnAt, index + 1);
        nyalkan$clock = HudClock.begin(nyalkan$layerDrawnAt[index], nyalkan$drawnAt);
        nyalkan$layerDrawnAt[index] = nyalkan$drawnAt;
        return original.call(bus, event);
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = POST, ordinal = 1))
    private Event nyalkan$guiPost(IEventBus bus, Event event, Operation<Event> original) {
        if (nyalkan$mode != null)
            nyalkan$closeSegment(((RenderGuiEvent) event).getGuiGraphics());
        return original.call(bus, event);
    }

    @Unique
    private static void nyalkan$layerDone() {
        if (nyalkan$drawnLayer < 0)
            return;

        HudClock.end(nyalkan$clock);
        nyalkan$clock = HudClock.NONE;
        HUD.recordCost(nyalkan$drawnLayer, System.nanoTime() - nyalkan$drawnAt);
        int index = nyalkan$drawnLayer;
        if (index >= nyalkan$leftStep.length) {
            nyalkan$leftStep = Arrays.copyOf(nyalkan$leftStep, index + 1);
            nyalkan$rightStep = Arrays.copyOf(nyalkan$rightStep, index + 1);
        }
        Gui gui = Minecraft.getInstance().gui;
        nyalkan$leftStep[index] = gui.leftHeight - nyalkan$leftAt;
        nyalkan$rightStep[index] = gui.rightHeight - nyalkan$rightAt;
        if (HUD.checkLayer(nyalkan$layer) && nyalkan$layer != null)
            LIVE_LAYERS.add(nyalkan$layer);
        nyalkan$drawnLayer = -1;
    }

    @Unique
    private static void nyalkan$closeSegment(GuiGraphics graphics) {
        if (!nyalkan$inSegment)
            return;

        nyalkan$layerDone();
        HUD.endCapture(graphics);
        HUD.composite(nyalkan$segment);
        nyalkan$segment++;
        nyalkan$inSegment = false;
    }
}
