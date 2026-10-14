package net.nyalkan.render.bake;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.nyalkan.render.vertex.TerrainRenderType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record BakedMesh(Map<TerrainRenderType, List<BakedQuad>> layers, boolean live) {
    public static BakedMesh of(TerrainRenderType renderType, List<BakedQuad> quads) {
        Map<TerrainRenderType, List<BakedQuad>> layers = new EnumMap<>(TerrainRenderType.class);
        layers.put(renderType, quads);
        return new BakedMesh(layers, false);
    }

    public BakedMesh keepLive() {
        return new BakedMesh(this.layers, true);
    }
}
