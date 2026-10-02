package net.nyalkan.mixin.render.batching;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeState.class)
public interface CompositeStateAccessor {

    @Accessor("transparencyState")
    RenderStateShard.TransparencyStateShard nyalkan$getTransparency();

    @Accessor("depthTestState")
    RenderStateShard.DepthTestStateShard nyalkan$getDepthTest();

    @Accessor("cullState")
    RenderStateShard.CullStateShard nyalkan$getCull();

    @Accessor("writeMaskState")
    RenderStateShard.WriteMaskStateShard nyalkan$getWriteMask();
}
