package net.nyalkan.mixin.tick;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.nyalkan.interfaces.ExtendedLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLightTickM extends Entity implements ExtendedLivingEntity {
    @Shadow protected int lerpSteps;
    @Shadow protected double lerpX;
    @Shadow protected double lerpY;
    @Shadow protected double lerpZ;
    @Shadow protected double lerpYRot;
    @Shadow protected double lerpXRot;
    @Shadow protected double lerpYHeadRot;
    @Shadow protected int lerpHeadSteps;
    @Shadow public float yBodyRot;
    @Shadow public float yBodyRotO;
    @Shadow public float yHeadRot;
    @Shadow public float yHeadRotO;

    @Shadow protected abstract void lerpHeadRotationStep(int steps, double targetYHeadRot);
    @Shadow public abstract void calculateEntityAnimation(boolean includeHeight);

    protected LivingEntityLightTickM(EntityType<?> type, Level level) {
        super(type, level);
    }

    // follow server movement only
    @Override
    public void nyalkan$lightTick() {
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            this.lerpHeadSteps--;
        }
        this.calculateEntityAnimation(false);
    }
}
