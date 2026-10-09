package net.nyalkan.mixin.texture.mip;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.nyalkan.Initializer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

// odd sprites cost mips
@Mixin(SpriteLoader.class)
public class SpriteLoaderM {
    @Unique private static final int MAX_SIDE = 4096;
    @Unique private static final long MAX_PIXELS = 4L << 20;

    @Shadow @Final private ResourceLocation location;

    @ModifyVariable(method = "stitch", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<SpriteContents> nyalkan$alignSprites(List<SpriteContents> sprites, List<SpriteContents> original, int mipLevel, Executor executor) {
        if (!Initializer.CONFIG.mipmapSpriteFix || mipLevel <= 0)
            return sprites;

        int align = 1 << mipLevel;
        List<SpriteContents> result = null;
        for (int i = 0; i < sprites.size(); i++) {
            SpriteContents sprite = sprites.get(i);
            SpriteContents scaled = nyalkan$scaled(sprite, align);
            if (scaled == sprite)
                continue;

            if (result == null)
                result = new ArrayList<>(sprites);
            result.set(i, scaled);
        }
        return result != null ? result : sprites;
    }

    @Unique
    private SpriteContents nyalkan$scaled(SpriteContents sprite, int align) {
        int width = sprite.width();
        int height = sprite.height();
        int fits = Math.min(Math.min(Integer.lowestOneBit(width), Integer.lowestOneBit(height)), align);
        if (fits >= align)
            return sprite;

        int factor = align / fits;
        NativeImage image = sprite.getOriginalImage();
        int newWidth = image.getWidth() * factor;
        int newHeight = image.getHeight() * factor;
        if (newWidth > MAX_SIDE || newHeight > MAX_SIDE || (long) newWidth * newHeight > MAX_PIXELS)
            return sprite;

        NativeImage upscaled = new NativeImage(image.format(), newWidth, newHeight, false);
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++)
                upscaled.setPixelRGBA(x, y, image.getPixelRGBA(x / factor, y / factor));
        }

        SpriteContents result = new SpriteContents(sprite.name(), new FrameSize(width * factor, height * factor), upscaled, sprite.metadata());
        sprite.close();
        Initializer.LOGGER.info("Scaled {} in {} from {}x{} by {}x", sprite.name(), this.location, width, height, factor);
        return result;
    }
}
