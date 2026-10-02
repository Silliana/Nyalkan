package net.nyalkan.mixin.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.nyalkan.mixin.render.batching.CompositeRenderTypeAccessor;
import net.nyalkan.mixin.render.batching.CompositeStateAccessor;
import net.nyalkan.interfaces.ExtendedRenderType;
import net.nyalkan.render.vertex.TerrainRenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderType.class)
public class RenderTypeM implements ExtendedRenderType {
    TerrainRenderType terrainRenderType;
    // avoid shadowing RenderStateShard name
    @Unique
    private String nyalkan$name;
    @Unique
    private Boolean nyalkan$batchable;
    @Unique
    private Boolean nyalkan$decal;
    @Unique
    private Boolean nyalkan$culls;
    @Unique
    private Boolean nyalkan$deferrable;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void inj(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2, CallbackInfo ci) {
        nyalkan$name = string;
        terrainRenderType = switch (string) {
            case "solid" -> TerrainRenderType.SOLID;
            case "cutout" -> TerrainRenderType.CUTOUT;
            case "cutout_mipped" -> TerrainRenderType.CUTOUT_MIPPED;
            case "translucent" -> TerrainRenderType.TRANSLUCENT;
            case "tripwire" -> TerrainRenderType.TRIPWIRE;
            default -> null;
        };
    }

    @Override
    public TerrainRenderType getTerrainRenderType() {
        return terrainRenderType;
    }

    @Override
    public String nyalkan$getName() {
        return nyalkan$name;
    }

    @Override
    public boolean nyalkan$isBatchable() {
        if (nyalkan$batchable == null)
            nyalkan$batchable = computeBatchable();
        return nyalkan$batchable;
    }

    @Override
    public boolean nyalkan$isDecal() {
        if (nyalkan$decal == null)
            nyalkan$decal = (Object) this instanceof CompositeRenderTypeAccessor composite
                    && ((CompositeStateAccessor) (Object) composite.nyalkan$getState()).nyalkan$getDepthTest() == RenderStateShard.EQUAL_DEPTH_TEST;
        return nyalkan$decal;
    }

    @Override
    public boolean nyalkan$culls() {
        if (nyalkan$culls == null)
            nyalkan$culls = (Object) this instanceof CompositeRenderTypeAccessor composite
                    && ((CompositeStateAccessor) (Object) composite.nyalkan$getState()).nyalkan$getCull() == RenderStateShard.CULL;
        return nyalkan$culls;
    }

    @Override
    public boolean nyalkan$isDeferrable() {
        if (nyalkan$deferrable == null)
            nyalkan$deferrable = !this.nyalkan$isBatchable() && (Object) this instanceof CompositeRenderTypeAccessor composite
                    && ((CompositeStateAccessor) (Object) composite.nyalkan$getState()).nyalkan$getDepthTest() == RenderStateShard.LEQUAL_DEPTH_TEST;
        return nyalkan$deferrable;
    }

    @Unique
    private boolean computeBatchable() {
        RenderType type = (RenderType) (Object) this;
        if (!(type instanceof CompositeRenderTypeAccessor composite) || type.sortOnUpload() || !type.canConsolidateConsecutiveGeometry())
            return false;

        CompositeStateAccessor state = (CompositeStateAccessor) (Object) composite.nyalkan$getState();
        return state.nyalkan$getTransparency() == RenderStateShard.NO_TRANSPARENCY
                && state.nyalkan$getDepthTest() == RenderStateShard.LEQUAL_DEPTH_TEST
                && state.nyalkan$getWriteMask() == RenderStateShard.COLOR_DEPTH_WRITE;
    }
}
