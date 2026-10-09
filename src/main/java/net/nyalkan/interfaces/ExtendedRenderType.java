package net.nyalkan.interfaces;

import net.nyalkan.render.vertex.TerrainRenderType;

public interface ExtendedRenderType {

    TerrainRenderType getTerrainRenderType();

    String nyalkan$getName();

    boolean nyalkan$isBatchable();

    boolean nyalkan$isDecal();

    boolean nyalkan$culls();

    boolean nyalkan$isDeferrable();
}
