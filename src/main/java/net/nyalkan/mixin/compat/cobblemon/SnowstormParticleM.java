package net.nyalkan.mixin.compat.cobblemon;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.nyalkan.render.DistanceScale;
import net.nyalkan.render.chunk.RenderSection;
import net.nyalkan.render.chunk.WorldRenderer;
import net.nyalkan.render.occlusion.HiZ;
import net.nyalkan.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

// skip unseeable effect particles
@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.client.render.SnowstormParticle", remap = false)
public abstract class SnowstormParticleM extends Particle {
    protected SnowstormParticleM(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Unique private static final double MAX_DISTANCE = 48.0;
    @Unique private static final int RECHECK_FRAMES = 4;
    @Unique private long nyalkan$checkedFrame = Long.MIN_VALUE / 2;
    @Unique private boolean nyalkan$wasHidden;
    @Unique private static final BlockPos.MutableBlockPos nyalkan$pos = new BlockPos.MutableBlockPos();

    @Unique private static final ClassValue<Boolean> POT = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            return type.getName().equals("com.cobblemon.mod.common.block.campfirepot.CampfireBlock");
        }
    };

    @WrapMethod(method = "render", require = 0)
    private void nyalkan$culledRender(VertexConsumer buffer, Camera camera, float partialTicks, Operation<Void> original) {
        if (!RenderSystem.isOnRenderThread() || !nyalkan$hiddenCached(camera)) {
            original.call(buffer, camera, partialTicks);
        }
    }

    @WrapMethod(method = "tick", require = 0)
    private void nyalkan$throttledTick(Operation<Void> original) {
        if ((this.age & 3) == 0 || !RenderSystem.isOnRenderThread()
                || !nyalkan$hiddenCached(Minecraft.getInstance().gameRenderer.getMainCamera())) {
            original.call();
            return;
        }
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (++this.age >= this.lifetime)
            this.remove();
    }

    @Unique
    private boolean nyalkan$hiddenCached(Camera camera) {
        long frame = Renderer.getFrameCount();
        if (frame - this.nyalkan$checkedFrame >= RECHECK_FRAMES) {
            this.nyalkan$wasHidden = nyalkan$hidden(camera);
            this.nyalkan$checkedFrame = frame;
        }
        return this.nyalkan$wasHidden;
    }

    @Unique
    private boolean nyalkan$hidden(Camera camera) {
        Vec3 pos = this.getPos();
        Vec3 eye = camera.getPosition();
        double dx = pos.x - eye.x, dy = pos.y - eye.y, dz = pos.z - eye.z;
        if (dx * dx + dy * dy + dz * dz > DistanceScale.sqr(MAX_DISTANCE))
            return true;

        WorldRenderer worldRenderer = WorldRenderer.getInstance();
        RenderSection section = worldRenderer.getSectionGrid().getSectionAtBlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        if (section != null && section.getLastFrame() != worldRenderer.getLastFrame())
            return true;

        if (HiZ.occluded(this.getBoundingBox()))
            return true;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return false;
        BlockPos block = nyalkan$pos.set(pos.x, pos.y, pos.z);
        return eye.y < block.getY() + 1.0 && POT.get(level.getBlockState(block).getBlock().getClass());
    }
}
