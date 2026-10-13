package net.nyalkan.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.nyalkan.render.chunk.build.thread.ThreadBuilderPack;
import net.nyalkan.render.vertex.CustomVertexFormat;
import net.nyalkan.render.vertex.TerrainRenderType;
import net.nyalkan.vulkan.shader.GraphicsPipeline;
import net.nyalkan.vulkan.shader.Pipeline;
import net.nyalkan.vulkan.shader.SPIRVUtils;

import java.util.function.Function;

import static net.nyalkan.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;

public abstract class PipelineManager {
    // union fs drops slash
    private static final String shaderPath = normalizeTrailingSlash(
            SPIRVUtils.class.getResource("/assets/nyalkan/shaders/").toExternalForm());

    private static String normalizeTrailingSlash(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
    public static VertexFormat TERRAIN_VERTEX_FORMAT;

    public static void setTerrainVertexFormat(VertexFormat format) {
        TERRAIN_VERTEX_FORMAT = format;
    }

    static GraphicsPipeline terrainShaderEarlyZ, terrainShader, fastBlitPipeline, fastBlitPipelineNonInverted;
    public static GraphicsPipeline particleInstancedPipeline;
    private static GraphicsPipeline cloudsPipeline;

    private static Function<TerrainRenderType, GraphicsPipeline> shaderGetter;

    public static void init() {
        setTerrainVertexFormat(CustomVertexFormat.COMPRESSED_TERRAIN);
        createBasicPipelines();
        setDefaultShader();
        ThreadBuilderPack.defaultTerrainBuilderConstructor();
    }

    public static void setDefaultShader() {
        setShaderGetter(renderType -> renderType == TerrainRenderType.TRANSLUCENT ? terrainShaderEarlyZ : terrainShader);
    }

    private static void createBasicPipelines() {
        terrainShaderEarlyZ = createPipeline("terrain","terrain", "terrain_Z", TERRAIN_VERTEX_FORMAT);
        terrainShader = createPipeline("terrain", "terrain", "terrain", TERRAIN_VERTEX_FORMAT);
        fastBlitPipeline = createPipeline("blit", "blit", "blit", CustomVertexFormat.NONE);
        fastBlitPipelineNonInverted = createPipeline("blit", "blit_noninverted", "blit", CustomVertexFormat.NONE);
        cloudsPipeline = createPipeline("clouds", "clouds", "clouds", DefaultVertexFormat.POSITION_COLOR);
        particleInstancedPipeline = createInstancedPipeline("particle_instanced", CustomVertexFormat.PARTICLE_QUAD, CustomVertexFormat.PARTICLE_INSTANCE);
    }

    private static GraphicsPipeline createInstancedPipeline(String name, VertexFormat vertexFormat, VertexFormat instanceFormat) {
        String path = String.format("basic/%s/%s", name, name);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(vertexFormat, path);
        pipelineBuilder.setInstanceFormat(instanceFormat);
        pipelineBuilder.parseBindingsJSON();
        pipelineBuilder.compileShaders();

        return pipelineBuilder.createGraphicsPipeline();
    }

    private static GraphicsPipeline createPipeline(String baseName, String vertName, String fragName,VertexFormat vertexFormat) {
        String pathB = String.format("basic/%s/%s", baseName, baseName);
        String pathV = String.format("basic/%s/%s", baseName, vertName);
        String pathF = String.format("basic/%s/%s", baseName, fragName);

        Pipeline.Builder pipelineBuilder = new Pipeline.Builder(vertexFormat, pathB);
        pipelineBuilder.parseBindingsJSON();

        SPIRVUtils.SPIRV vertShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.vsh", shaderPath, pathV), SPIRVUtils.ShaderKind.VERTEX_SHADER);
        SPIRVUtils.SPIRV fragShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.fsh", shaderPath, pathF), SPIRVUtils.ShaderKind.FRAGMENT_SHADER);
        pipelineBuilder.setSPIRVs(vertShaderSPIRV, fragShaderSPIRV);

        return pipelineBuilder.createGraphicsPipeline();
    }

    public static GraphicsPipeline getTerrainShader(TerrainRenderType renderType) {
        return shaderGetter.apply(renderType);
    }

    public static void setShaderGetter(Function<TerrainRenderType, GraphicsPipeline> consumer) {
        shaderGetter = consumer;
    }

    public static GraphicsPipeline getTerrainDirectShader(RenderType renderType) {
        return terrainShader;
    }

    public static GraphicsPipeline getTerrainIndirectShader(RenderType renderType) {
        return terrainShaderEarlyZ;
    }

    public static GraphicsPipeline getFastBlitPipeline() { return fastBlitPipeline; }

    public static GraphicsPipeline getFastBlitPipelineNonInverted() { return fastBlitPipelineNonInverted; }

    public static GraphicsPipeline getCloudsPipeline() { return cloudsPipeline; }

    public static void destroyPipelines() {
        terrainShaderEarlyZ.cleanUp();
        terrainShader.cleanUp();
        fastBlitPipeline.cleanUp();
        fastBlitPipelineNonInverted.cleanUp();
        cloudsPipeline.cleanUp();
    }
}
