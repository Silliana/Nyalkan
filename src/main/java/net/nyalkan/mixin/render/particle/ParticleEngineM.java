package net.nyalkan.mixin.render.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.world.phys.AABB;
import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.render.particle.GpuParticles;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public class ParticleEngineM {

    @Unique private static final ClassValue<Boolean> VANILLA_BOX = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return type.getMethod("getRenderBoundingBox", float.class).getDeclaringClass() == SingleQuadParticle.class;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    };

    @Inject(method = "tick", at = @At("RETURN"))
    private void nyalkan$invalidateGpu(CallbackInfo ci) {
        GpuParticles.invalidate();
    }

    @Inject(method = "clearParticles", at = @At("HEAD"))
    private void nyalkan$clearGpu(CallbackInfo ci) {
        GpuParticles.clear();
    }

    @Inject(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V", at = @At("HEAD"))
    private void nyalkan$flushDeferred(CallbackInfo ci) {
        ImmediateBatcher.INSTANCE.flush();
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Queue;iterator()Ljava/util/Iterator;"))
    private Iterator<Particle> nyalkan$gpuParticles(Queue<Particle> queue, Operation<Iterator<Particle>> original,
                                                     @Local ParticleRenderType type, @Local(argsOnly = true) Camera camera,
                                                     @Local(argsOnly = true) float partialTick) {
        if (!GpuParticles.handles(type) || !RenderSystem.isOnRenderThread())
            return original.call(queue);
        return GpuParticles.render(type, queue, camera, partialTick);
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/Particle;getRenderBoundingBox(F)Lnet/minecraft/world/phys/AABB;"))
    private AABB nyalkan$quadBox(Particle particle, float partialTick, Operation<AABB> original) {
        if (particle instanceof SingleQuadParticle && VANILLA_BOX.get(particle.getClass()))
            return AABB.INFINITE;
        return original.call(particle, partialTick);
    }
}
