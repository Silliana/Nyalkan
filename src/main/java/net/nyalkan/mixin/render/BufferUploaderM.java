package net.nyalkan.mixin.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.ShaderInstance;
import net.nyalkan.interfaces.ShaderMixed;
import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.VRenderSystem;
import net.nyalkan.vulkan.shader.GraphicsPipeline;

import net.nyalkan.vulkan.shader.Pipeline;
import net.nyalkan.vulkan.texture.VTextureSelector;
import net.nyalkan.vulkan.util.DrawDebug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BufferUploader.class)
public class BufferUploaderM {

    @Overwrite
    public static void reset() {}

    @Overwrite
    public static void drawWithShader(MeshData meshData) {
        RenderSystem.assertOnRenderThread();
        ImmediateBatcher.INSTANCE.flushGui();

        MeshData.DrawState parameters = meshData.drawState();

        Renderer renderer = Renderer.getInstance();

        if (DrawDebug.ENABLED) {
            if (parameters.vertexCount() > 0) DrawDebug.drawWithShader++;
            else DrawDebug.emptyMesh++;
        }

        if (parameters.vertexCount() > 0) {
            ShaderInstance shaderInstance = RenderSystem.getShader();
            GraphicsPipeline pipeline = ((ShaderMixed)(shaderInstance)).getPipeline();

            if (pipeline == null)
                throw new NullPointerException("Shader %s has no initialized pipeline".formatted(shaderInstance.getName()));

            // superset formats allowed
            if (!pipeline.canReadFormat(parameters.format())) {
                ShaderInstance fallback = nyalkan$fallbackShader(parameters.format());
                GraphicsPipeline fallbackPipeline = fallback != null ? ((ShaderMixed) fallback).getPipeline() : null;
                if (fallbackPipeline == null || !fallbackPipeline.canReadFormat(parameters.format())) {
                    if (DrawDebug.ENABLED) DrawDebug.formatSkip++;
                    meshData.close();
                    return;
                }
                shaderInstance = fallback;
                pipeline = fallbackPipeline;
            }

            // Used to update legacy shader uniforms
            // TODO it would be faster to allocate a buffer from stack and set all values
            shaderInstance.apply();

            VRenderSystem.setPrimitiveTopologyGL(parameters.mode().asGLMode);
            renderer.bindGraphicsPipeline(pipeline, parameters.format());
            VTextureSelector.bindShaderTextures(pipeline);
            renderer.uploadAndBindUBOs(pipeline);
            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

    @Overwrite
    public static void draw(MeshData meshData) {
        ImmediateBatcher.INSTANCE.flushGui();
        MeshData.DrawState parameters = meshData.drawState();

        if (parameters.vertexCount() > 0) {
            if (DrawDebug.ENABLED) DrawDebug.draw++;
            Renderer renderer = Renderer.getInstance();
            Pipeline pipeline = renderer.getBoundPipeline();

            // rebind at draw time
            if (pipeline instanceof GraphicsPipeline graphicsPipeline) {
                VRenderSystem.setPrimitiveTopologyGL(parameters.mode().asGLMode);
                renderer.bindGraphicsPipeline(graphicsPipeline);
            }

            renderer.uploadAndBindUBOs(pipeline);

            Renderer.getDrawer().draw(meshData.vertexBuffer(), parameters.mode(), parameters.format(), parameters.vertexCount());
        }

        meshData.close();
    }

    @Unique
    private static ShaderInstance nyalkan$fallbackShader(com.mojang.blaze3d.vertex.VertexFormat format) {
        if (format == com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION)
            return net.minecraft.client.renderer.GameRenderer.getPositionShader();
        if (format == com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR)
            return net.minecraft.client.renderer.GameRenderer.getPositionColorShader();
        if (format == com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX)
            return net.minecraft.client.renderer.GameRenderer.getPositionTexShader();
        if (format == com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX_COLOR)
            return net.minecraft.client.renderer.GameRenderer.getPositionTexColorShader();
        return null;
    }
}
