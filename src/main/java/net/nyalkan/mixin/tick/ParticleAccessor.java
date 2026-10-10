package net.nyalkan.mixin.tick;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {
    @Accessor("x") double nyalkan$x();
    @Accessor("y") double nyalkan$y();
    @Accessor("z") double nyalkan$z();
    @Accessor("xo") void nyalkan$setXo(double value);
    @Accessor("yo") void nyalkan$setYo(double value);
    @Accessor("zo") void nyalkan$setZo(double value);
    @Accessor("age") int nyalkan$age();
    @Accessor("age") void nyalkan$setAge(int value);
    @Accessor("lifetime") int nyalkan$lifetime();
}
