package net.nyalkan.mixin.tick;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ParticleEngine.class)
public interface ParticleEngineInvoker {
    @Invoker("tickParticle")
    void nyalkan$tickParticle(Particle particle);
}
