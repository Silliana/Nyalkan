package net.nyalkan.mixin.compat;

import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.nyalkan.render.compat.ResourceExistsCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ReloadableResourceManager.class)
public class ResourceReloadM {
    @Inject(method = "createReload", at = @At("HEAD"))
    private void nyalkan$clearExists(CallbackInfoReturnable<?> cir) {
        ResourceExistsCache.clear();
    }
}
