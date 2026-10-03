package net.nyalkan.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SpriteContents.AnimatedTexture.class)
public interface AnimatedTextureAccessor {

    @Invoker("uploadFrame")
    void nyalkan$uploadFrame(int x, int y, int frameIndex);
}
