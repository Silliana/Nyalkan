package net.nyalkan.mixin.compat.journeymap;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.nyalkan.render.gui.HudCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "journeymap.client.ui.UIManager", remap = false)
public abstract class UIManagerM {
    @Unique private static final int MOVING_HZ = 30;
    @Unique private static final int STILL_HZ = 15;
    @Unique private static final long SETTLE = 250_000_000L;

    @Unique private static final HudCache nyalkan$minimapCache = new HudCache("minimap", MOVING_HZ);
    @Unique private static double nyalkan$x, nyalkan$z;
    @Unique private static float nyalkan$yaw;
    @Unique private static long nyalkan$movedAt;

    @WrapMethod(method = "drawMiniMap", require = 0)
    private void nyalkan$cachedMiniMap(GuiGraphics graphics, Operation<Void> original) {
        if (!RenderSystem.isOnRenderThread()) {
            original.call(graphics);
            return;
        }
        nyalkan$minimapCache.setRate(nyalkan$moving() ? MOVING_HZ : STILL_HZ);
        nyalkan$minimapCache.render(graphics, () -> original.call(graphics));
    }

    @Unique
    private static boolean nyalkan$moving() {
        long now = System.nanoTime();
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null) {
            double x = camera.getX(), z = camera.getZ();
            float yaw = camera.getYRot();
            if (x != nyalkan$x || z != nyalkan$z || yaw != nyalkan$yaw) {
                nyalkan$x = x;
                nyalkan$z = z;
                nyalkan$yaw = yaw;
                nyalkan$movedAt = now;
            }
        }
        return now - nyalkan$movedAt < SETTLE;
    }
}
