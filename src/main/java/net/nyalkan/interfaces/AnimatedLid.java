package net.nyalkan.interfaces;

import net.minecraft.world.level.block.entity.ChestLidController;

public interface AnimatedLid {
    ChestLidController nyalkan$lid();

    boolean nyalkan$lidAnimating();

    void nyalkan$setLidAnimating(boolean animating);
}
