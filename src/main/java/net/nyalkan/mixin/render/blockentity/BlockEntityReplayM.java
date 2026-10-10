package net.nyalkan.mixin.render.blockentity;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.nyalkan.interfaces.ExtendedBlockEntity;
import net.nyalkan.render.bake.BlockEntityBakery;
import net.nyalkan.render.model.OutputReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntity.class)
public class BlockEntityReplayM implements ExtendedBlockEntity {
    @Unique private OutputReplay.Entry nyalkan$replay;
    @Unique private Object nyalkan$renderer;
    @Unique private int nyalkan$rendererEpoch = -1;
    @Unique private volatile Object nyalkan$bakeKey;

    @Override
    public Object nyalkan$renderer(int epoch) {
        return this.nyalkan$rendererEpoch == epoch ? this.nyalkan$renderer : null;
    }

    @Override
    public void nyalkan$setRenderer(Object renderer, int epoch) {
        this.nyalkan$renderer = renderer;
        this.nyalkan$rendererEpoch = epoch;
    }

    @Override
    public Object nyalkan$bakeKey() {
        return this.nyalkan$bakeKey;
    }

    @Override
    public void nyalkan$setBakeKey(Object key) {
        this.nyalkan$bakeKey = key;
    }

    @Override
    public OutputReplay.Entry nyalkan$replay() {
        return this.nyalkan$replay;
    }

    @Override
    public void nyalkan$setReplay(OutputReplay.Entry entry) {
        this.nyalkan$replay = entry;
    }

    @Inject(method = {"loadWithComponents", "loadCustomOnly", "setChanged()V", "setBlockState"}, at = @At("HEAD"))
    private void nyalkan$invalidate(CallbackInfo ci) {
        if (this.nyalkan$replay != null)
            OutputReplay.refreshSoon(this.nyalkan$replay);
    }

    @Inject(method = "loadWithComponents", at = @At("TAIL"))
    private void nyalkan$rebake(CallbackInfo ci) {
        BlockEntityBakery.refresh((BlockEntity) (Object) this);
    }
}
