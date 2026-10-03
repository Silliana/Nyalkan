package net.nyalkan.mixin.texture.tracking;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.nyalkan.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public class GuiGraphicsTrackingM {

    @Inject(method = "blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", at = @At("HEAD"))
    private void nyalkan$markSprite(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite, CallbackInfo ci) {
        SpriteUtil.markActive(sprite);
    }

    @Inject(method = "blit(IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;FFFF)V", at = @At("HEAD"))
    private void nyalkan$markTintedSprite(int x, int y, int blitOffset, int width, int height, TextureAtlasSprite sprite,
                                            float red, float green, float blue, float alpha, CallbackInfo ci) {
        SpriteUtil.markActive(sprite);
    }
}
