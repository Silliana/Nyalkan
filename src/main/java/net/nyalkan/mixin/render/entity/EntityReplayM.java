package net.nyalkan.mixin.render.entity;

import net.minecraft.world.entity.Entity;
import net.nyalkan.interfaces.ExtendedEntity;
import net.nyalkan.render.model.OutputReplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public class EntityReplayM implements ExtendedEntity {
    @Unique private OutputReplay.Entry nyalkan$replay;
    @Unique private int nyalkan$drawnTick;

    @Override
    public OutputReplay.Entry nyalkan$replay() {
        return this.nyalkan$replay;
    }

    @Override
    public void nyalkan$setReplay(OutputReplay.Entry entry) {
        this.nyalkan$replay = entry;
    }

    @Override
    public int nyalkan$drawnTick() {
        return this.nyalkan$drawnTick;
    }

    @Override
    public void nyalkan$setDrawnTick(int tick) {
        this.nyalkan$drawnTick = tick;
    }
}
