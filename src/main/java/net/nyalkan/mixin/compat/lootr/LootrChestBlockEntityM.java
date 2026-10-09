package net.nyalkan.mixin.compat.lootr;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.state.BlockState;
import net.nyalkan.render.bake.BlockEntityBakery;
import net.nyalkan.render.bake.ChestBaker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "noobanidus.mods.lootr.common.block.entity.LootrChestBlockEntity", remap = false)
public class LootrChestBlockEntityM {
    @Shadow @Final private ChestLidController chestLidController;

    @Inject(method = "defaultTick", at = @At("TAIL"))
    private void nyalkan$tickLid(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        ChestBaker.refreshLid((BlockEntity) (Object) this);
    }

    @Inject(method = "triggerEvent", at = @At("RETURN"))
    private void nyalkan$lidEvent(int id, int type, CallbackInfoReturnable<Boolean> cir) {
        ChestBaker.refreshLid((BlockEntity) (Object) this);
    }

    @Inject(method = "setClientOpened", at = @At("RETURN"))
    private void nyalkan$opened(boolean opened, CallbackInfo ci) {
        BlockEntityBakery.invalidate((BlockEntity) (Object) this);
    }

    public ChestLidController nyalkan$lid() {
        return this.chestLidController;
    }
}
