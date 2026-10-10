package net.nyalkan.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.nyalkan.tick.TickSpreader;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ParticleEngine.class)
public class ParticleTickM {

    @WrapMethod(method = "tickParticle")
    private void nyalkan$spreadTick(Particle particle, Operation<Void> original) {
        if (!TickSpreader.deferParticle((ParticleEngine) (Object) this, particle))
            original.call(particle);
    }
}
