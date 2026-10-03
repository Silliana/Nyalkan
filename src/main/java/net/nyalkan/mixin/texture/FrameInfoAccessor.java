package net.nyalkan.mixin.texture;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.FrameInfo.class)
public interface FrameInfoAccessor {

    @Accessor("index")
    int nyalkan$getIndex();
}
