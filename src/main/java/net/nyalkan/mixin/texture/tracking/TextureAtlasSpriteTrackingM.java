package net.nyalkan.mixin.texture.tracking;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.nyalkan.render.texture.SpriteUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public class TextureAtlasSpriteTrackingM {

    @Inject(method = "wrap", at = @At("HEAD"))
    private void nyalkan$markSprite(VertexConsumer consumer, CallbackInfoReturnable<VertexConsumer> cir) {
        SpriteUtil.markActive((TextureAtlasSprite) (Object) this);
    }
}
