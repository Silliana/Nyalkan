package net.nyalkan.mixin.compat.simulated;

import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TEMP TODO port shader framebuffer
@Pseudo
@Mixin(targets = "dev.simulated_team.simulated.content.end_sea.EndSeaRenderer", remap = false)
public class EndSeaRendererM {
    @Unique private static final ResourceLocation nyalkan$shadows = ResourceLocation.fromNamespaceAndPath("simulated", "end_sea_shadows");
    @Unique private static final ResourceLocation nyalkan$shader = ResourceLocation.fromNamespaceAndPath("simulated", "end_sea");

    @Inject(method = "renderLayers", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nyalkan$skipLayers(CallbackInfo ci) {
        if (VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(nyalkan$shadows) == null
                || VeilRenderSystem.setShader(nyalkan$shader) == null)
            ci.cancel();
    }
}
