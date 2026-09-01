package net.nyalkan.mixin.chunk;

import net.minecraft.client.renderer.SectionBufferBuilderPool;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// empty pool not null
@Mixin(SectionBufferBuilderPool.class)
public class SectionBufferBuilderPoolM {

    @ModifyVariable(method = "allocate", at = @At("STORE"), ordinal = 1)
    private static int skipAllocation(int value) {
        return 0;
    }
}
