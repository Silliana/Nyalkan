package net.nyalkan.mixin.compat.cobblemon;

import net.nyalkan.render.bake.BlockEntityBakery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// pot body is baked
@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.client.render.block.CampfireBlockEntityRenderer", remap = false)
public class CookingPotRendererM {
    @Inject(method = "renderPot", at = @At("HEAD"), cancellable = true, require = 0)
    private void nyalkan$skipBaked(CallbackInfo ci) {
        if (BlockEntityBakery.skipBaked())
            ci.cancel();
    }
}
