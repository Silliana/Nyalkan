package net.nyalkan.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedSpriteContents;
import net.nyalkan.render.texture.SpriteUtil;
import net.nyalkan.vulkan.util.DrawDebug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// skip hidden sprite animation
@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$Ticker")
public abstract class MSpriteTicker {

    @Shadow int frame;
    @Shadow int subFrame;
    @Shadow @Final SpriteContents.AnimatedTexture animationInfo;
    @Unique private ExtendedSpriteContents nyalkan$contents;
    @Unique private boolean nyalkan$stale;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nyalkan$captureContents(SpriteContents contents, SpriteContents.AnimatedTexture animationInfo,
                                           @Coerce Object interpolationData, CallbackInfo ci) {
        this.nyalkan$contents = (ExtendedSpriteContents) contents;
    }

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void nyalkan$skipHidden(int x, int y, CallbackInfo ci) {
        if (!Initializer.CONFIG.animateOnlyVisible)
            return;

        if (this.nyalkan$contents.nyalkan$isActive()) {
            if (this.nyalkan$stale && SpriteUtil.shouldUpload()) {
                this.nyalkan$stale = false;
                int index = ((FrameInfoAccessor) this.animationInfo.frames.get(this.frame)).nyalkan$getIndex();
                ((AnimatedTextureAccessor) this.animationInfo).nyalkan$uploadFrame(x, y, index);
            }

            if (DrawDebug.ENABLED) DrawDebug.spritesTicked++;
            return;
        }

        List<SpriteContents.FrameInfo> frames = this.animationInfo.frames;
        if (++this.subFrame >= frames.get(this.frame).time) {
            this.frame = (this.frame + 1) % frames.size();
            this.subFrame = 0;
        }

        this.nyalkan$stale = true;
        if (DrawDebug.ENABLED) DrawDebug.spritesSkipped++;
        ci.cancel();
    }

    @Inject(method = "tickAndUpload", at = @At("RETURN"))
    private void nyalkan$clearActive(int x, int y, CallbackInfo ci) {
        if (SpriteUtil.shouldUpload())
            this.nyalkan$contents.nyalkan$setActive(false);
    }
}
