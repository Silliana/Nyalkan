package net.nyalkan.mixin.render.batching;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.nyalkan.interfaces.ExtendedRenderType;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.render.immediate.ImmediateBatcher;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.SequencedMap;

@Mixin(MultiBufferSource.BufferSource.class)
public abstract class BufferSourceM {

    @Shadow @Final protected SequencedMap<RenderType, ByteBufferBuilder> fixedBuffers;
    @Shadow @Nullable protected RenderType lastSharedType;

    @Shadow public abstract void endBatch(RenderType renderType);

    // after other head hooks
    @Inject(method = "getBuffer", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0), cancellable = true)
    private void nyalkan$batch(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        if (!nyalkan$isMainSource())
            return;

        ImmediateBatcher batcher = ImmediateBatcher.INSTANCE;
        if (batcher.isGuiMode()) {
            cir.setReturnValue(batcher.getBuffer(renderType, this::nyalkan$closeOpenShared));
            return;
        }

        if (this.fixedBuffers.containsKey(renderType))
            return;

        ExtendedRenderType type = (ExtendedRenderType) renderType;
        if (type.nyalkan$isBatchable())
            cir.setReturnValue(batcher.getBuffer(renderType, this::nyalkan$closeOpenShared));
        else if (type.nyalkan$isDeferrable())
            cir.setReturnValue(batcher.getDeferredBuffer(renderType, this::nyalkan$closeOpenShared));
        else
            // vanilla would flush here
            batcher.flush();
    }

    @Inject(method = {"endLastBatch", "endBatch()V", "endBatch(Lnet/minecraft/client/renderer/RenderType;)V"}, at = @At("HEAD"))
    private void nyalkan$flush(CallbackInfo ci) {
        if (nyalkan$isMainSource() && !ImmediateBatcher.INSTANCE.isGuiMode())
            ImmediateBatcher.INSTANCE.flush();
    }

    @Unique
    private void nyalkan$closeOpenShared() {
        if (this.lastSharedType != null)
            this.endBatch(this.lastSharedType);
    }

    @Inject(method = "getBuffer", at = @At("RETURN"))
    private void nyalkan$tagCull(RenderType renderType, CallbackInfoReturnable<VertexConsumer> cir) {
        if ((Object) this == Minecraft.getInstance().renderBuffers().bufferSource())
            ImmediateBatcher.INSTANCE.touchExternal();
        if (cir.getReturnValue() instanceof ExtendedVertexBuilder builder)
            builder.setCullsBackFaces(((ExtendedRenderType) renderType).nyalkan$culls());
    }

    @Unique
    private boolean nyalkan$isMainSource() {
        return ImmediateBatcher.isEnabled() && !ImmediateBatcher.INSTANCE.isSuspended() && (Object) this == Minecraft.getInstance().renderBuffers().bufferSource();
    }
}
