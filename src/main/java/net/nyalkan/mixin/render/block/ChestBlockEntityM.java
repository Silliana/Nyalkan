package net.nyalkan.mixin.render.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.state.BlockState;
import net.nyalkan.interfaces.AnimatedLid;
import net.nyalkan.render.bake.ChestBaker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityM implements AnimatedLid {
    @Shadow @Final private ChestLidController chestLidController;
    @Unique private volatile boolean nyalkan$lidAnimating;

    @Inject(method = "lidAnimateTick", at = @At("TAIL"))
    private static void nyalkan$tickLid(Level level, BlockPos pos, BlockState state, ChestBlockEntity blockEntity, CallbackInfo ci) {
        ChestBaker.refreshLid(blockEntity);
    }

    @Inject(method = "triggerEvent", at = @At("RETURN"))
    private void nyalkan$lidEvent(int id, int type, CallbackInfoReturnable<Boolean> cir) {
        ChestBaker.refreshLid((ChestBlockEntity) (Object) this);
    }

    @Override
    public ChestLidController nyalkan$lid() {
        return this.chestLidController;
    }

    @Override
    public boolean nyalkan$lidAnimating() {
        return this.nyalkan$lidAnimating;
    }

    @Override
    public void nyalkan$setLidAnimating(boolean animating) {
        this.nyalkan$lidAnimating = animating;
    }
}
