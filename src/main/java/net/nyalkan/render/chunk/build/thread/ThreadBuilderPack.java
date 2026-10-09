package net.nyalkan.render.chunk.build.thread;

import net.nyalkan.render.vertex.TerrainBufferBuilder;
import net.nyalkan.render.vertex.TerrainRenderType;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

public class ThreadBuilderPack {
    private static Function<TerrainRenderType, TerrainBufferBuilder> terrainBuilderConstructor;

    public static void defaultTerrainBuilderConstructor() {
        terrainBuilderConstructor = renderType -> new TerrainBufferBuilder(TerrainRenderType.getRenderType(renderType).bufferSize());
    }

    public static void setTerrainBuilderConstructor(Function<TerrainRenderType, TerrainBufferBuilder> constructor) {
        terrainBuilderConstructor = constructor;
    }

    private final Map<TerrainRenderType, TerrainBufferBuilder> builders;

    public ThreadBuilderPack() {
        var map = new EnumMap<TerrainRenderType, TerrainBufferBuilder>(TerrainRenderType.class);
        Arrays.stream(TerrainRenderType.values()).forEach(
                terrainRenderType -> map.put(terrainRenderType,
                        terrainBuilderConstructor.apply(terrainRenderType))
        );
        builders = map;
    }

    public TerrainBufferBuilder builder(TerrainRenderType renderType) {
        return this.builders.get(renderType);
    }

    public void abortAll() {
        this.builders.values().forEach(TerrainBufferBuilder::reset);
    }

    public void clearAll() {
        this.builders.values().forEach(TerrainBufferBuilder::clear);
    }

    public void freeAll() {
        this.builders.values().forEach(TerrainBufferBuilder::free);
    }

}
