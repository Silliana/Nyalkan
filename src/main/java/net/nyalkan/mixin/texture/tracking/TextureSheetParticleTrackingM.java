package net.nyalkan.mixin.texture.tracking;

import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.nyalkan.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureSheetParticle.class)
public class TextureSheetParticleTrackingM {

    @Shadow protected TextureAtlasSprite sprite;

    @Inject(method = "getU0", at = @At("HEAD"))
    private void nyalkan$markSprite(CallbackInfoReturnable<Float> cir) {
        SpriteUtil.markActive(this.sprite);
    }
}
