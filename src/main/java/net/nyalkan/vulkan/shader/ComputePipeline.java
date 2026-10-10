package net.nyalkan.vulkan.shader;

import net.nyalkan.vulkan.device.DeviceManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

// single stage compute
public class ComputePipeline {
    private final long shaderModule;
    private final long layout;
    private final long handle;

    public ComputePipeline(String name, String glsl, long descriptorSetLayout, int pushConstantBytes) {
        SPIRVUtils.SPIRV spirv = SPIRVUtils.compileShader(name, glsl, SPIRVUtils.ShaderKind.COMPUTE_SHADER);
        this.shaderModule = Pipeline.createShaderModule(spirv.bytecode());

        try (MemoryStack stack = stackPush()) {
            VkPipelineLayoutCreateInfo layoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            layoutInfo.sType$Default();
            layoutInfo.pSetLayouts(stack.longs(descriptorSetLayout));
            if (pushConstantBytes > 0) {
                VkPushConstantRange.Buffer range = VkPushConstantRange.calloc(1, stack);
                range.stageFlags(VK_SHADER_STAGE_COMPUTE_BIT);
                range.offset(0);
                range.size(pushConstantBytes);
                layoutInfo.pPushConstantRanges(range);
            }

            LongBuffer pLayout = stack.mallocLong(1);
            if (vkCreatePipelineLayout(DeviceManager.vkDevice, layoutInfo, null, pLayout) != VK_SUCCESS)
                throw new RuntimeException("Failed to create compute pipeline layout " + name);
            this.layout = pLayout.get(0);

            VkPipelineShaderStageCreateInfo stage = VkPipelineShaderStageCreateInfo.calloc(stack);
            stage.sType$Default();
            stage.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            stage.module(this.shaderModule);
            stage.pName(stack.UTF8("main"));

            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType$Default();
            pipelineInfo.stage(stage);
            pipelineInfo.layout(this.layout);

            LongBuffer pPipeline = stack.mallocLong(1);
            if (vkCreateComputePipelines(DeviceManager.vkDevice, VK_NULL_HANDLE, pipelineInfo, null, pPipeline) != VK_SUCCESS)
                throw new RuntimeException("Failed to create compute pipeline " + name);
            this.handle = pPipeline.get(0);
        }
        spirv.free();
    }

    public long getLayout() {
        return this.layout;
    }

    public void bind(VkCommandBuffer commandBuffer) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, this.handle);
    }
}
