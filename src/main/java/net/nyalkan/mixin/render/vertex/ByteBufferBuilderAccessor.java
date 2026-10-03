package net.nyalkan.mixin.render.vertex;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ByteBufferBuilder.class)
public interface ByteBufferBuilderAccessor {

    @Accessor("pointer")
    long nyalkan$pointer();

    @Accessor("writeOffset")
    int nyalkan$writeOffset();
}
