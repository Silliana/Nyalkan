package net.nyalkan.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.nyalkan.tick.TickSpreader;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$RebindableTickingBlockEntityWrapper")
public abstract class BlockTickerM implements TickingBlockEntity {

    @WrapMethod(method = "tick")
    private void nyalkan$spreadTick(Operation<Void> original) {
        if (RenderSystem.isOnRenderThread() && TickSpreader.deferBlock(this))
            return;
        original.call();
    }
}
