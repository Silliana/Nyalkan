package net.nyalkan.vulkan.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.nyalkan.interfaces.VertexFormatMixed;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.Vulkan;
import net.nyalkan.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class GraphicsPipeline extends Pipeline {
    private final Object2LongMap<PipelineState> graphicsPipelines = new Object2LongOpenHashMap<>();

    private final VertexFormat vertexFormat;
    private final VertexFormat instanceFormat;
    private final VertexInputDescription vertexInputDescription;

    private final Map<VertexFormat, AdaptedLayout> adaptedLayouts = new HashMap<>();

    private long vertShaderModule = 0;
    private long fragShaderModule = 0;

    GraphicsPipeline(Builder builder) {
        super(builder.shaderPath);
        this.buffers = builder.UBOs;
        this.manualUBO = builder.manualUBO;
        this.imageDescriptors = builder.imageDescriptors;
        this.pushConstants = builder.pushConstants;
        this.vertexFormat = builder.vertexFormat;
        this.instanceFormat = builder.instanceFormat;

        this.vertexInputDescription = new VertexInputDescription(this.vertexFormat, this.instanceFormat);

        createDescriptorSetLayout();
        createPipelineLayout();
        createShaderModules(builder.vertShaderSPIRV, builder.fragShaderSPIRV);

        if (builder.renderPass != null)
            getHandle(PipelineState.DEFAULT);

        createDescriptorSets(Renderer.getFramesNum());

        PIPELINES.add(this);
    }

    public long getHandle(PipelineState state) {
        ToLongFunction<PipelineState> create = s -> createGraphicsPipeline(s, vertexInputDescription);
        return graphicsPipelines.computeIfAbsent(state, create);
    }

    public long getHandle(PipelineState state, VertexFormat bufferFormat) {
        if (bufferFormat == vertexFormat)
            return getHandle(state);

        AdaptedLayout layout = adaptedLayouts.computeIfAbsent(bufferFormat,
                f -> new AdaptedLayout(VertexInputDescription.adapted(vertexFormat, f)));
        ToLongFunction<PipelineState> create = s -> createGraphicsPipeline(s, layout.description);
        return layout.pipelines.computeIfAbsent(state, create);
    }

    public boolean canReadFormat(VertexFormat bufferFormat) {
        if (bufferFormat == vertexFormat)
            return true;
        if (instanceFormat != null)
            return false;

        for (VertexFormatElement element : vertexFormat.getElements()) {
            if (!bufferFormat.contains(element))
                return false;
        }
        return true;
    }

    private long createGraphicsPipeline(PipelineState state, VertexInputDescription vertexInputDescription) {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer entryPoint = stack.UTF8("main");

            VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2, stack);

            VkPipelineShaderStageCreateInfo vertShaderStageInfo = shaderStages.get(0);

            vertShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            vertShaderStageInfo.stage(VK_SHADER_STAGE_VERTEX_BIT);
            vertShaderStageInfo.module(vertShaderModule);
            vertShaderStageInfo.pName(entryPoint);

            VkPipelineShaderStageCreateInfo fragShaderStageInfo = shaderStages.get(1);

            fragShaderStageInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
            fragShaderStageInfo.stage(VK_SHADER_STAGE_FRAGMENT_BIT);
            fragShaderStageInfo.module(fragShaderModule);
            fragShaderStageInfo.pName(entryPoint);
            if ((state.blendState_i & PipelineState.BlendState.OPAQUE_ALPHA_BIT) != 0) {
                VkSpecializationMapEntry.Buffer entries = VkSpecializationMapEntry.calloc(1, stack);
                entries.get(0).constantID(SPIRVUtils.OPAQUE_ALPHA_CONSTANT).offset(0).size(4);
                fragShaderStageInfo.pSpecializationInfo(VkSpecializationInfo.calloc(stack)
                        .pMapEntries(entries)
                        .pData(stack.malloc(4).putInt(0, 1)));
            }

            // ===> VERTEX STAGE <===

            VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc(stack);
            vertexInputInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            vertexInputInfo.pVertexBindingDescriptions(vertexInputDescription.bindingDescriptions);
            vertexInputInfo.pVertexAttributeDescriptions(vertexInputDescription.attributeDescriptions);

            // ===> ASSEMBLY STAGE <===

            final int topology = PipelineState.AssemblyRasterState.decodeTopology(state.assemblyRasterState);

            VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack);
            inputAssembly.sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            inputAssembly.topology(topology);
            inputAssembly.primitiveRestartEnable(false);

            // ===> VIEWPORT & SCISSOR

            VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc(stack);
            viewportState.sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);

            viewportState.viewportCount(1);
            viewportState.scissorCount(1);

            // ===> RASTERIZATION STAGE <===

            final int polygonMode = PipelineState.AssemblyRasterState.decodePolygonMode(state.assemblyRasterState);
            final int cullMode = PipelineState.AssemblyRasterState.decodeCullMode(state.assemblyRasterState);
            final int frontFace = PipelineState.AssemblyRasterState.decodeFrontFace(state.assemblyRasterState);

            VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc(stack);
            rasterizer.sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            rasterizer.depthClampEnable(false);
            rasterizer.rasterizerDiscardEnable(false);
            rasterizer.polygonMode(polygonMode);
            rasterizer.lineWidth(1.0f);
            rasterizer.cullMode(cullMode);
            rasterizer.frontFace(frontFace);
            rasterizer.depthBiasEnable(true);

            // ===> MULTISAMPLING <===

            VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc(stack);
            multisampling.sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            multisampling.sampleShadingEnable(false);
            multisampling.rasterizationSamples(VK_SAMPLE_COUNT_1_BIT);

            // ===> DEPTH TEST <===

            VkPipelineDepthStencilStateCreateInfo depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack);
            depthStencil.sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            depthStencil.depthTestEnable(PipelineState.DepthState.depthTest(state.depthState_i));
            depthStencil.depthWriteEnable(PipelineState.DepthState.depthMask(state.depthState_i));
            depthStencil.depthCompareOp(PipelineState.DepthState.decodeDepthFun(state.depthState_i));
            depthStencil.depthBoundsTestEnable(false);
            depthStencil.minDepthBounds(0.0f); // Optional
            depthStencil.maxDepthBounds(1.0f); // Optional

            // ref and masks dynamic
            depthStencil.stencilTestEnable(PipelineState.StencilState.testEnable(state.stencilState_i));
            VkStencilOpState stencilOpState = VkStencilOpState.calloc(stack)
                    .failOp(PipelineState.StencilState.decodeFailOp(state.stencilState_i))
                    .passOp(PipelineState.StencilState.decodePassOp(state.stencilState_i))
                    .depthFailOp(PipelineState.StencilState.decodeDepthFailOp(state.stencilState_i))
                    .compareOp(PipelineState.StencilState.decodeCompareOp(state.stencilState_i))
                    .compareMask(0xFF)
                    .writeMask(0xFF)
                    .reference(0);
            depthStencil.front(stencilOpState);
            depthStencil.back(stencilOpState);

            // ===> COLOR BLENDING <===

            VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack);
            colorBlendAttachment.colorWriteMask(state.colorMask_i);

            if (PipelineState.BlendState.enable(state.blendState_i)) {
                colorBlendAttachment.blendEnable(true);
                colorBlendAttachment.srcColorBlendFactor(PipelineState.BlendState.getSrcRgbFactor(state.blendState_i));
                colorBlendAttachment.dstColorBlendFactor(PipelineState.BlendState.getDstRgbFactor(state.blendState_i));
                colorBlendAttachment.colorBlendOp(VK_BLEND_OP_ADD);
                colorBlendAttachment.srcAlphaBlendFactor(PipelineState.BlendState.getSrcAlphaFactor(state.blendState_i));
                colorBlendAttachment.dstAlphaBlendFactor(PipelineState.BlendState.getDstAlphaFactor(state.blendState_i));
                colorBlendAttachment.alphaBlendOp(VK_BLEND_OP_ADD);
            }
            else {
                colorBlendAttachment.blendEnable(false);
            }

            VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc(stack);
            colorBlending.sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            colorBlending.logicOpEnable(PipelineState.LogicOpState.enable(state.logicOp_i));
            colorBlending.logicOp(PipelineState.LogicOpState.decodeFun(state.logicOp_i));
            colorBlending.pAttachments(colorBlendAttachment);
            colorBlending.blendConstants(stack.floats(0.0f, 0.0f, 0.0f, 0.0f));

            // ===> DYNAMIC STATES <===

            VkPipelineDynamicStateCreateInfo dynamicStates = VkPipelineDynamicStateCreateInfo.calloc(stack);
            dynamicStates.sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);

            if (topology == VK_PRIMITIVE_TOPOLOGY_LINE_LIST || polygonMode == VK_POLYGON_MODE_LINE)
                dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR, VK_DYNAMIC_STATE_LINE_WIDTH,
                        VK_DYNAMIC_STATE_STENCIL_REFERENCE, VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK, VK_DYNAMIC_STATE_STENCIL_WRITE_MASK, VK_DYNAMIC_STATE_BLEND_CONSTANTS));
            else
                dynamicStates.pDynamicStates(stack.ints(VK_DYNAMIC_STATE_DEPTH_BIAS, VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR,
                        VK_DYNAMIC_STATE_STENCIL_REFERENCE, VK_DYNAMIC_STATE_STENCIL_COMPARE_MASK, VK_DYNAMIC_STATE_STENCIL_WRITE_MASK, VK_DYNAMIC_STATE_BLEND_CONSTANTS));

            VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            pipelineInfo.pStages(shaderStages);
            pipelineInfo.pVertexInputState(vertexInputInfo);
            pipelineInfo.pInputAssemblyState(inputAssembly);
            pipelineInfo.pViewportState(viewportState);
            pipelineInfo.pRasterizationState(rasterizer);
            pipelineInfo.pMultisampleState(multisampling);
            pipelineInfo.pDepthStencilState(depthStencil);
            pipelineInfo.pColorBlendState(colorBlending);
            pipelineInfo.pDynamicState(dynamicStates);
            pipelineInfo.layout(pipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);

            if (!Vulkan.DYNAMIC_RENDERING) {
                pipelineInfo.renderPass(state.renderPass.getId());
                pipelineInfo.subpass(0);
            }
            else {
                //dyn-rendering
                VkPipelineRenderingCreateInfoKHR renderingInfo = VkPipelineRenderingCreateInfoKHR.calloc(stack);
                renderingInfo.sType(KHRDynamicRendering.VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO_KHR);
                renderingInfo.pColorAttachmentFormats(stack.ints(state.renderPass.getFramebuffer().getFormat()));
                renderingInfo.depthAttachmentFormat(state.renderPass.getFramebuffer().getDepthFormat());
                pipelineInfo.pNext(renderingInfo);
            }

            LongBuffer pGraphicsPipeline = stack.mallocLong(1);

            if (vkCreateGraphicsPipelines(DeviceManager.vkDevice, PIPELINE_CACHE, pipelineInfo, null, pGraphicsPipeline) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            return pGraphicsPipeline.get(0);
        }
    }

    private void createShaderModules(SPIRVUtils.SPIRV vertSpirv, SPIRVUtils.SPIRV fragSpirv) {
        this.vertShaderModule = createShaderModule(vertSpirv.bytecode());
        this.fragShaderModule = createShaderModule(fragSpirv.bytecode());
    }

    public void cleanUp() {
        vkDestroyShaderModule(DeviceManager.vkDevice, vertShaderModule, null);
        vkDestroyShaderModule(DeviceManager.vkDevice, fragShaderModule, null);

        vertexInputDescription.cleanUp();

        adaptedLayouts.forEach((format, layout) -> {
            layout.description.cleanUp();
            layout.pipelines.forEach((state, pipeline) -> vkDestroyPipeline(DeviceManager.vkDevice, pipeline, null));
        });
        adaptedLayouts.clear();

        destroyDescriptorSets();

        graphicsPipelines.forEach((state, pipeline) -> {
            vkDestroyPipeline(DeviceManager.vkDevice, pipeline, null);
        });
        graphicsPipelines.clear();

        vkDestroyDescriptorSetLayout(DeviceManager.vkDevice, descriptorSetLayout, null);
        vkDestroyPipelineLayout(DeviceManager.vkDevice, pipelineLayout, null);

        PIPELINES.remove(this);
        Renderer.getInstance().removeUsedPipeline(this);
    }

    static class VertexInputDescription {
        final VkVertexInputAttributeDescription.Buffer attributeDescriptions;
        final VkVertexInputBindingDescription.Buffer bindingDescriptions;

        // optional instance binding
        VertexInputDescription(VertexFormat vertexFormat, VertexFormat instanceFormat) {
            if (instanceFormat == null) {
                this.bindingDescriptions = getBindingDescription(vertexFormat, 0, VK_VERTEX_INPUT_RATE_VERTEX);
                this.attributeDescriptions = getAttributeDescriptions(vertexFormat, 0, 0);
                return;
            }

            VkVertexInputBindingDescription.Buffer vertexBinding = getBindingDescription(vertexFormat, 0, VK_VERTEX_INPUT_RATE_VERTEX);
            VkVertexInputBindingDescription.Buffer instanceBinding = getBindingDescription(instanceFormat, 1, VK_VERTEX_INPUT_RATE_INSTANCE);
            this.bindingDescriptions = VkVertexInputBindingDescription.calloc(2);
            this.bindingDescriptions.put(0, vertexBinding.get(0));
            this.bindingDescriptions.put(1, instanceBinding.get(0));
            MemoryUtil.memFree(vertexBinding);
            MemoryUtil.memFree(instanceBinding);

            int vertexCount = vertexFormat.getElements().size();
            int instanceCount = instanceFormat.getElements().size();
            VkVertexInputAttributeDescription.Buffer vertexAttrs = getAttributeDescriptions(vertexFormat, 0, 0);
            VkVertexInputAttributeDescription.Buffer instanceAttrs = getAttributeDescriptions(instanceFormat, 1, vertexCount);
            this.attributeDescriptions = VkVertexInputAttributeDescription.calloc(vertexCount + instanceCount);
            for (int i = 0; i < vertexCount; ++i)
                this.attributeDescriptions.put(i, vertexAttrs.get(i));
            for (int i = 0; i < instanceCount; ++i)
                this.attributeDescriptions.put(vertexCount + i, instanceAttrs.get(i));
            MemoryUtil.memFree(vertexAttrs);
            MemoryUtil.memFree(instanceAttrs);
        }

        static VertexInputDescription adapted(VertexFormat shaderFormat, VertexFormat bufferFormat) {
            VkVertexInputAttributeDescription.Buffer attributes = getAttributeDescriptions(shaderFormat, 0, 0);

            List<VertexFormatElement> elements = shaderFormat.getElements();
            for (int i = 0; i < elements.size(); ++i)
                attributes.get(i).offset(bufferFormat.getOffset(elements.get(i)));

            return new VertexInputDescription(attributes, getBindingDescription(bufferFormat, 0, VK_VERTEX_INPUT_RATE_VERTEX));
        }

        private VertexInputDescription(VkVertexInputAttributeDescription.Buffer attributeDescriptions, VkVertexInputBindingDescription.Buffer bindingDescriptions) {
            this.attributeDescriptions = attributeDescriptions;
            this.bindingDescriptions = bindingDescriptions;
        }

        void cleanUp() {
            MemoryUtil.memFree(this.bindingDescriptions);
            MemoryUtil.memFree(this.attributeDescriptions);
        }
    }

    private record AdaptedLayout(VertexInputDescription description, Object2LongMap<PipelineState> pipelines) {
        AdaptedLayout(VertexInputDescription description) {
            this(description, new Object2LongOpenHashMap<>());
        }
    }

    private static VkVertexInputBindingDescription.Buffer getBindingDescription(VertexFormat vertexFormat, int binding, int inputRate) {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.calloc(1);

        bindingDescription.binding(binding);
        bindingDescription.stride(vertexFormat.getVertexSize());
        bindingDescription.inputRate(inputRate);

        return bindingDescription;
    }

    private static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(VertexFormat vertexFormat, int binding, int startLocation) {
        List<VertexFormatElement> elements = vertexFormat.getElements();

        int size = elements.size();

        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.calloc(size);

        int offset = 0;

        for (int i = 0; i < size; ++i) {
            VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(i);
            posDescription.binding(binding);
            posDescription.location(startLocation + i);

            VertexFormatElement formatElement = elements.get(i);
            VertexFormatElement.Usage usage = formatElement.usage();
            VertexFormatElement.Type type = formatElement.type();
            int elementCount = formatElement.count();

            switch (usage) {
                case POSITION -> {
                    switch (type) {
                        case FLOAT -> {
                            posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
                            posDescription.offset(offset);

                            offset += 12;
                        }
                        case SHORT -> {
                            posDescription.format(VK_FORMAT_R16G16B16A16_SINT);
                            posDescription.offset(offset);

                            offset += 8;
                        }
                        case BYTE -> {
                            posDescription.format(VK_FORMAT_R8G8B8A8_SINT);
                            posDescription.offset(offset);

                            offset += 4;
                        }
                    }

                }

                case COLOR -> {
                    posDescription.format(VK_FORMAT_R8G8B8A8_UNORM);
                    posDescription.offset(offset);

                    offset += 4;
                }

                case UV -> {
                    switch (type) {
                        case FLOAT -> {
                            posDescription.format(VK_FORMAT_R32G32_SFLOAT);
                            posDescription.offset(offset);

                            offset += 8;
                        }
                        case SHORT -> {
                            posDescription.format(VK_FORMAT_R16G16_SINT);
                            posDescription.offset(offset);

                            offset += 4;
                        }
                        case USHORT -> {
                            posDescription.format(VK_FORMAT_R16G16_UINT);
                            posDescription.offset(offset);

                            offset += 4;
                        }
                    }
                }

                case NORMAL -> {
                    posDescription.format(VK_FORMAT_R8G8B8A8_SNORM);
                    posDescription.offset(offset);

                    offset += 4;
                }

                case GENERIC -> {
                    if (type == VertexFormatElement.Type.SHORT && elementCount == 1) {
                        posDescription.format(VK_FORMAT_R16_SINT);
                        posDescription.offset(offset);

                        offset += 2;
                    }
                    else if (type == VertexFormatElement.Type.INT && elementCount == 1) {
                        posDescription.format(VK_FORMAT_R32_SINT);
                        posDescription.offset(offset);

                        offset += 4;
                    }
                    // mat4 instance rows
                    else if (type == VertexFormatElement.Type.FLOAT && elementCount == 4) {
                        posDescription.format(VK_FORMAT_R32G32B32A32_SFLOAT);
                        posDescription.offset(offset);

                        offset += 16;
                    }
                    else {
                        throw new RuntimeException(String.format("Unknown format: %s", usage));
                    }
                }

                default -> throw new RuntimeException(String.format("Unknown format: %s", usage));
            }

            posDescription.offset(((VertexFormatMixed) (vertexFormat)).getOffset(i));
        }

        return attributeDescriptions.rewind();
    }
}
