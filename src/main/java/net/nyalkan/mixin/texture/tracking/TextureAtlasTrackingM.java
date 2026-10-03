package net.nyalkan.mixin.texture.tracking;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.nyalkan.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlas.class)
public class TextureAtlasTrackingM {

    @Inject(method = "getSprite", at = @At("RETURN"))
    private void nyalkan$markSprite(CallbackInfoReturnable<TextureAtlasSprite> cir) {
        SpriteUtil.markActive(cir.getReturnValue());
    }
}
