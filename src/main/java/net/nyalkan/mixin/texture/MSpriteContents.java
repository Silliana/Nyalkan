package net.nyalkan.mixin.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.nyalkan.interfaces.ExtendedSpriteContents;
import net.nyalkan.render.texture.SpriteUtil;
import net.nyalkan.vulkan.texture.SpriteUploadBatch;
import net.nyalkan.vulkan.texture.VTextureSelector;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpriteContents.class)
public class MSpriteContents implements ExtendedSpriteContents {

    @Shadow @Final @Nullable SpriteContents.AnimatedTexture animatedTexture;

    @Unique private boolean nyalkan$active;

    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void checkUpload(int i, int j, int k, int l, NativeImage[] nativeImages, CallbackInfo ci) {
        if(!SpriteUtil.shouldUpload()) {
            ci.cancel();
            return;
        }

        SpriteUploadBatch.beginSprite();
        if (!SpriteUploadBatch.isActive())
            SpriteUtil.addTransitionedLayout(VTextureSelector.getBoundTexture(0));
    }

    @Inject(method = "upload", at = @At("RETURN"))
    private void endUpload(int i, int j, int k, int l, NativeImage[] nativeImages, CallbackInfo ci) {
        SpriteUploadBatch.endSprite();
    }

    @Override
    public boolean nyalkan$isAnimated() {
        return this.animatedTexture != null;
    }

    @Override
    public boolean nyalkan$isActive() {
        return this.nyalkan$active;
    }

    @Override
    public void nyalkan$setActive(boolean active) {
        this.nyalkan$active = active;
    }
}
