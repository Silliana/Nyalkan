package net.vulkanmod.render;

import net.vulkanmod.render.vertex.TerrainRenderType;

import java.util.HashMap;
import java.util.Map;

public class CustomTerrainShaders {
    public record Entry(TerrainRenderType reuseBucket, GraphicsPipelineSupplier pipeline) {
    }

    // mod shader built later
    public interface GraphicsPipelineSupplier {
        net.vulkanmod.vulkan.shader.GraphicsPipeline get();
    }

    private static final Map<String, Entry> registry = new HashMap<>();

    public static void register(String renderTypeName, TerrainRenderType reuseBucket, GraphicsPipelineSupplier pipeline) {
        registry.put(renderTypeName, new Entry(reuseBucket, pipeline));
    }

    public static Entry get(String renderTypeName) {
        return registry.get(renderTypeName);
    }
}
