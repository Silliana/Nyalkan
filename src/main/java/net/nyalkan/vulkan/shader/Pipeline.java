package net.nyalkan.vulkan.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.util.GsonHelper;
import net.neoforged.fml.loading.FMLPaths;
import net.nyalkan.Initializer;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.Vulkan;
import net.nyalkan.vulkan.device.DeviceManager;
import net.nyalkan.vulkan.framebuffer.RenderPass;
import net.nyalkan.vulkan.memory.MemoryManager;
import net.nyalkan.vulkan.memory.UniformBuffer;
import net.nyalkan.vulkan.shader.SPIRVUtils.SPIRV;
import net.nyalkan.vulkan.shader.SPIRVUtils.ShaderKind;
import net.nyalkan.vulkan.shader.descriptor.ImageDescriptor;
import net.nyalkan.vulkan.shader.descriptor.ManualUBO;
import net.nyalkan.vulkan.shader.descriptor.UBO;
import net.nyalkan.vulkan.shader.layout.AlignedStruct;
import net.nyalkan.vulkan.shader.layout.PushConstants;
import net.nyalkan.vulkan.texture.VTextureSelector;
import net.nyalkan.vulkan.texture.VulkanImage;
import org.apache.commons.lang3.Validate;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static net.nyalkan.vulkan.shader.SPIRVUtils.compileShader;
import static net.nyalkan.vulkan.shader.SPIRVUtils.compileShaderAbsoluteFile;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public abstract class Pipeline {

    private static final VkDevice DEVICE = Vulkan.getVkDevice();
    protected static final long PIPELINE_CACHE = createPipelineCache();
    protected static final List<Pipeline> PIPELINES = new LinkedList<>();

    // driver validates cache header
    private static Path getPipelineCacheFile() {
        return FMLPaths.CONFIGDIR.get().resolve("nyalkan_pipeline_cache.bin");
    }

    private static long createPipelineCache() {
        try (MemoryStack stack = stackPush()) {

            VkPipelineCacheCreateInfo cacheCreateInfo = VkPipelineCacheCreateInfo.calloc(stack);
            cacheCreateInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO);

            try {
                byte[] cacheData = Files.readAllBytes(getPipelineCacheFile());
                ByteBuffer pInitialData = MemoryUtil.memAlloc(cacheData.length).put(cacheData).flip();
                cacheCreateInfo.pInitialData(pInitialData);
            } catch (IOException e) {
                // first launch
            }

            LongBuffer pPipelineCache = stack.mallocLong(1);

            if (vkCreatePipelineCache(DEVICE, cacheCreateInfo, null, pPipelineCache) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create graphics pipeline");
            }

            if (cacheCreateInfo.pInitialData() != null)
                MemoryUtil.memFree(cacheCreateInfo.pInitialData());

            return pPipelineCache.get(0);
        }
    }

    public static void destroyPipelineCache() {
        savePipelineCache();
        vkDestroyPipelineCache(DEVICE, PIPELINE_CACHE, null);
    }

    private static void savePipelineCache() {
        try (MemoryStack stack = stackPush()) {
            PointerBuffer pDataSize = stack.mallocPointer(1);
            if (vkGetPipelineCacheData(DEVICE, PIPELINE_CACHE, pDataSize, null) != VK_SUCCESS)
                return;

            long dataSize = pDataSize.get(0);
            if (dataSize == 0)
                return;

            ByteBuffer pData = MemoryUtil.memAlloc((int) dataSize);
            try {
                if (vkGetPipelineCacheData(DEVICE, PIPELINE_CACHE, pDataSize, pData) != VK_SUCCESS)
                    return;

                byte[] bytes = new byte[(int) dataSize];
                pData.get(bytes);
                Files.write(getPipelineCacheFile(), bytes);
            } finally {
                MemoryUtil.memFree(pData);
            }
        } catch (IOException e) {
            Initializer.LOGGER.warn("Nyalkan: failed to save pipeline cache to disk", e);
        }
    }

    public static void recreateDescriptorSets(int frames) {
        PIPELINES.forEach(pipeline -> {
            pipeline.destroyDescriptorSets();
            pipeline.createDescriptorSets(frames);
        });
    }

    public final String name;

    protected long descriptorSetLayout;
    protected long pipelineLayout;

    protected DescriptorSets[] descriptorSets;
    protected List<UBO> buffers;
    protected ManualUBO manualUBO;
    protected List<ImageDescriptor> imageDescriptors;
    protected PushConstants pushConstants;

    public Pipeline(String name) {
        this.name = name;
    }

    protected void createDescriptorSetLayout() {
        try (MemoryStack stack = stackPush()) {
            int bindingsSize = this.buffers.size() + imageDescriptors.size();

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.calloc(bindingsSize, stack);

            for (UBO ubo : this.buffers) {
                VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(ubo.getBinding());
                uboLayoutBinding.binding(ubo.getBinding());
                uboLayoutBinding.descriptorCount(1);
                uboLayoutBinding.descriptorType(ubo.getType());
                uboLayoutBinding.pImmutableSamplers(null);
                uboLayoutBinding.stageFlags(ubo.getStages());
            }

            for (ImageDescriptor imageDescriptor : this.imageDescriptors) {
                VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(imageDescriptor.getBinding());
                samplerLayoutBinding.binding(imageDescriptor.getBinding());
                samplerLayoutBinding.descriptorCount(1);
                samplerLayoutBinding.descriptorType(imageDescriptor.getType());
                samplerLayoutBinding.pImmutableSamplers(null);
                samplerLayoutBinding.stageFlags(imageDescriptor.getStages());
            }

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.calloc(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            if (vkCreateDescriptorSetLayout(DeviceManager.vkDevice, layoutInfo, null, pDescriptorSetLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor set layout");
            }

            this.descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    protected void createPipelineLayout() {
        try (MemoryStack stack = stackPush()) {
            // ===> PIPELINE LAYOUT CREATION <===

            VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc(stack);
            pipelineLayoutInfo.sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO);
            pipelineLayoutInfo.pSetLayouts(stack.longs(this.descriptorSetLayout));

            if (this.pushConstants != null) {
                VkPushConstantRange.Buffer pushConstantRange = VkPushConstantRange.calloc(1, stack);
                pushConstantRange.size(this.pushConstants.getSize());
                pushConstantRange.offset(0);
                pushConstantRange.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

                pipelineLayoutInfo.pPushConstantRanges(pushConstantRange);
            }

            LongBuffer pPipelineLayout = stack.longs(VK_NULL_HANDLE);

            if (vkCreatePipelineLayout(DEVICE, pipelineLayoutInfo, null, pPipelineLayout) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create pipeline layout");
            }

            pipelineLayout = pPipelineLayout.get(0);
        }
    }

    protected void createDescriptorSets(int frames) {
        descriptorSets = new DescriptorSets[frames];
        for (int i = 0; i < frames; ++i) {
            descriptorSets[i] = new DescriptorSets(this);
        }
    }

    public void scheduleCleanUp() {
        MemoryManager.getInstance().addFrameOp(this::cleanUp);
    }

    public abstract void cleanUp();

    void destroyDescriptorSets() {
        for (DescriptorSets descriptorSets : this.descriptorSets) {
            descriptorSets.cleanUp();
        }

        this.descriptorSets = null;
    }

    public ManualUBO getManualUBO() {
        return this.manualUBO;
    }

    public void resetDescriptorPool(int i) {
        if (this.descriptorSets != null)
            this.descriptorSets[i].resetIdx();

    }

    public PushConstants getPushConstants() {
        return this.pushConstants;
    }

    public long getLayout() {
        return pipelineLayout;
    }

    public List<ImageDescriptor> getImageDescriptors() {
        return imageDescriptors;
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, int frame) {
        UniformBuffer uniformBuffer = Renderer.getDrawer().getUniformBuffer();
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS);
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int frame) {
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS);
    }

    static long createShaderModule(ByteBuffer spirvCode) {

        try (MemoryStack stack = stackPush()) {

            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.calloc(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            if (vkCreateShaderModule(DEVICE, createInfo, null, pShaderModule) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create shader module");
            }

            return pShaderModule.get(0);
        }
    }

    private static VkCommandBuffer boundCommandBuffer;
    private static long boundLayout, boundSet;
    private static final int[] boundOffsets = new int[16];
    private static int boundOffsetCount = -1;

    public static void invalidateBoundSets() {
        boundCommandBuffer = null;
    }

    protected static class DescriptorSets {
        private final Pipeline pipeline;
        private final long[] lastUB;
        private final int[] lastEpoch;
        private final int[] lastOffset;

        private static final int REUSE_WINDOW = 16;
        private final int keySize;
        private final long[] key;
        private long[] writtenKeys = new long[0];
        private int writtenCount;
        private int poolSize = 10;
        private long descriptorPool;
        private LongBuffer sets;
        private long currentSet;
        private int currentIdx = -1;

        private final long[] boundUBs;
        private final ImageDescriptor.State[] boundTextures;
        private final IntBuffer dynamicOffsets;

        DescriptorSets(Pipeline pipeline) {
            this.pipeline = pipeline;
            this.boundTextures = new ImageDescriptor.State[pipeline.imageDescriptors.size()];
            this.dynamicOffsets = MemoryUtil.memAllocInt(pipeline.buffers.size());
            this.boundUBs = new long[pipeline.buffers.size()];
            this.lastUB = new long[pipeline.buffers.size()];
            this.lastEpoch = new int[pipeline.buffers.size()];
            this.lastOffset = new int[pipeline.buffers.size()];
            this.keySize = pipeline.buffers.size() + pipeline.imageDescriptors.size() * 2;
            this.key = new long[this.keySize];

            Arrays.setAll(boundTextures, i -> new ImageDescriptor.State(0, 0));

            try (MemoryStack stack = stackPush()) {
                this.createDescriptorPool(stack);
                this.createDescriptorSets(stack);
            }
        }

        protected void bindSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int bindPoint) {
            try (MemoryStack stack = stackPush()) {

                this.updateUniforms(uniformBuffer);
                this.updateDescriptorSet(stack, uniformBuffer);

                if (isBound(commandBuffer))
                    return;

                vkCmdBindDescriptorSets(commandBuffer, bindPoint, pipeline.pipelineLayout,
                        0, stack.longs(currentSet), dynamicOffsets);
            }
        }

        private boolean isBound(VkCommandBuffer commandBuffer) {
            int count = this.dynamicOffsets.limit();
            boolean same = commandBuffer == boundCommandBuffer && pipeline.pipelineLayout == boundLayout
                    && this.currentSet == boundSet && count == boundOffsetCount && count <= boundOffsets.length;
            for (int i = 0; same && i < count; i++)
                same = boundOffsets[i] == this.dynamicOffsets.get(i);
            if (same)
                return true;

            boundCommandBuffer = commandBuffer;
            boundLayout = pipeline.pipelineLayout;
            boundSet = this.currentSet;
            boundOffsetCount = count <= boundOffsets.length ? count : -1;
            for (int i = 0; i < count && i < boundOffsets.length; i++)
                boundOffsets[i] = this.dynamicOffsets.get(i);
            return false;
        }

        private static boolean sameBytes(long a, long b, int size) {
            int i = 0;
            for (; i + 8 <= size; i += 8)
                if (MemoryUtil.memGetLong(a + i) != MemoryUtil.memGetLong(b + i))
                    return false;
            for (; i < size; i++)
                if (MemoryUtil.memGetByte(a + i) != MemoryUtil.memGetByte(b + i))
                    return false;
            return true;
        }

        private void updateUniforms(UniformBuffer globalUB) {
            int i = 0;
            for (UBO ubo : pipeline.buffers) {
                boolean useOwnUB = ubo.getUniformBuffer() != null;
                UniformBuffer ub = useOwnUB ? ubo.getUniformBuffer() : globalUB;

                // TODO: non mappable memory

                int alignedSize = UniformBuffer.getAlignedSize(ubo.getSize());
                ub.checkCapacity(alignedSize);

                int currentOffset = (int) ub.getUsedBytes();

                if (!useOwnUB) {
                    long ptr = ub.getPointer();
                    ubo.update(ptr);

                    if (this.lastUB[i] == ub.getId() && this.lastEpoch[i] == ub.getEpoch()
                            && sameBytes(ub.getBasePointer() + this.lastOffset[i], ptr, ubo.getSize())) {
                        currentOffset = this.lastOffset[i];
                    } else {
                        this.lastUB[i] = ub.getId();
                        this.lastEpoch[i] = ub.getEpoch();
                        this.lastOffset[i] = currentOffset;
                        ub.updateOffset(alignedSize);
                    }
                }

                this.dynamicOffsets.put(i, currentOffset);

                ++i;
            }
        }

        private boolean needsUpdate(UniformBuffer uniformBuffer) {
            if (currentIdx == -1)
                return true;

            for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = image.getSampler();

                if (imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                if (!this.boundTextures[j].isCurrentState(view, sampler)) {
                    return true;
                }
            }

            for (int j = 0; j < pipeline.buffers.size(); ++j) {
                UBO ubo = pipeline.buffers.get(j);
                UniformBuffer uniformBufferI = ubo.getUniformBuffer();

                if (uniformBufferI == null)
                    uniformBufferI = uniformBuffer;

                if (this.boundUBs[j] != uniformBufferI.getId()) {
                    return true;
                }
            }

            return false;
        }

        private void buildKey(UniformBuffer uniformBuffer) {
            int k = 0;
            for (int j = 0; j < pipeline.buffers.size(); ++j) {
                UniformBuffer ub = pipeline.buffers.get(j).getUniformBuffer();
                this.key[k++] = (ub != null ? ub : uniformBuffer).getId();
            }

            for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                if (imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                this.key[k++] = imageDescriptor.getImageView(image);
                this.key[k++] = image.getSampler();
            }
        }

        private boolean reuseWrittenSet(UniformBuffer uniformBuffer) {
            this.buildKey(uniformBuffer);

            int keySize = this.keySize;
            int oldest = Math.max(0, this.writtenCount - REUSE_WINDOW);
            for (int idx = this.writtenCount - 1; idx >= oldest; --idx) {
                if (!Arrays.equals(this.writtenKeys, idx * keySize, idx * keySize + keySize, this.key, 0, keySize))
                    continue;

                this.currentSet = this.sets.get(idx);
                int k = 0;
                for (int j = 0; j < this.boundUBs.length; ++j)
                    this.boundUBs[j] = this.key[k++];
                for (ImageDescriptor.State state : this.boundTextures) {
                    state.set(this.key[k], this.key[k + 1]);
                    k += 2;
                }
                return true;
            }
            return false;
        }

        private void recordWrittenSet() {
            int needed = (this.currentIdx + 1) * this.keySize;
            if (this.writtenKeys.length < needed)
                this.writtenKeys = Arrays.copyOf(this.writtenKeys, Math.max(needed, this.writtenKeys.length * 2));

            System.arraycopy(this.key, 0, this.writtenKeys, this.currentIdx * this.keySize, this.keySize);
            this.writtenCount = this.currentIdx + 1;
        }

        private void checkPoolSize(MemoryStack stack) {
            if (this.currentIdx >= this.poolSize) {
                this.poolSize *= 2;

                this.createDescriptorPool(stack);
                this.createDescriptorSets(stack);
                this.currentIdx = 0;
            }
        }

        private void updateDescriptorSet(MemoryStack stack, UniformBuffer uniformBuffer) {

            //Check if update is needed
            if (!needsUpdate(uniformBuffer))
                return;

            if (reuseWrittenSet(uniformBuffer))
                return;

            this.currentIdx++;

            //Check pool size
            if (this.currentIdx >= this.poolSize)
                this.writtenCount = 0;
            checkPoolSize(stack);

            this.currentSet = this.sets.get(this.currentIdx);
            this.recordWrittenSet();

            VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.calloc(pipeline.buffers.size() + pipeline.imageDescriptors.size(), stack);
            VkDescriptorBufferInfo.Buffer[] bufferInfos = new VkDescriptorBufferInfo.Buffer[pipeline.buffers.size()];

            //TODO maybe ubo update is not needed everytime
            int i = 0;
            for (UBO ubo : pipeline.buffers) {
                UniformBuffer ub = ubo.getUniformBuffer();
                if (ub == null)
                    ub = uniformBuffer;
                boundUBs[i] = ub.getId();

                bufferInfos[i] = VkDescriptorBufferInfo.calloc(1, stack);
                bufferInfos[i].buffer(boundUBs[i]);
                bufferInfos[i].range(ubo.getSize());

                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(i);
                uboDescriptorWrite.sType$Default();
                uboDescriptorWrite.dstBinding(ubo.getBinding());
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(ubo.getType());
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfos[i]);
                uboDescriptorWrite.dstSet(currentSet);

                ++i;
            }

            VkDescriptorImageInfo.Buffer[] imageInfo = new VkDescriptorImageInfo.Buffer[pipeline.imageDescriptors.size()];

            for (int j = 0; j < pipeline.imageDescriptors.size(); ++j) {
                ImageDescriptor imageDescriptor = pipeline.imageDescriptors.get(j);
                VulkanImage image = imageDescriptor.getImage();
                long view = imageDescriptor.getImageView(image);
                long sampler = image.getSampler();
                int layout = imageDescriptor.getLayout();

                if (imageDescriptor.isReadOnlyLayout)
                    image.readOnlyLayout();

                imageInfo[j] = VkDescriptorImageInfo.calloc(1, stack);
                imageInfo[j].imageLayout(layout);
                imageInfo[j].imageView(view);

                if (imageDescriptor.useSampler)
                    imageInfo[j].sampler(sampler);

                VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(i);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(imageDescriptor.getBinding());
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(imageDescriptor.getType());
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo[j]);
                samplerDescriptorWrite.dstSet(currentSet);

                this.boundTextures[j].set(view, sampler);
                ++i;
            }

            vkUpdateDescriptorSets(DEVICE, descriptorWrites, null);
        }

        private void createDescriptorSets(MemoryStack stack) {
            LongBuffer layout = stack.mallocLong(this.poolSize);
//            layout.put(0, descriptorSetLayout);

            for (int i = 0; i < this.poolSize; ++i) {
                layout.put(i, pipeline.descriptorSetLayout);
            }

            VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.calloc(stack);
            allocInfo.sType$Default();
            allocInfo.descriptorPool(descriptorPool);
            allocInfo.pSetLayouts(layout);

            this.sets = MemoryUtil.memAllocLong(this.poolSize);

            int result = vkAllocateDescriptorSets(DEVICE, allocInfo, this.sets);
            if (result != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate descriptor sets. Result:" + result);
            }
        }

        private void createDescriptorPool(MemoryStack stack) {
            int size = pipeline.buffers.size() + pipeline.imageDescriptors.size();

            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc(size, stack);

            int i;
            for (i = 0; i < pipeline.buffers.size(); ++i) {
                VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(i);
//                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER_DYNAMIC);
                uniformBufferPoolSize.descriptorCount(this.poolSize);
            }

            for (; i < pipeline.buffers.size() + pipeline.imageDescriptors.size(); ++i) {
                VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(i);
                textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                textureSamplerPoolSize.descriptorCount(this.poolSize);
            }

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.calloc(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(this.poolSize);

            LongBuffer pDescriptorPool = stack.mallocLong(1);

            if (vkCreateDescriptorPool(DEVICE, poolInfo, null, pDescriptorPool) != VK_SUCCESS) {
                throw new RuntimeException("Failed to create descriptor pool");
            }

            if (this.descriptorPool != VK_NULL_HANDLE) {
                final long oldDescriptorPool = this.descriptorPool;
                MemoryManager.getInstance().addFrameOp(() -> {
                    vkDestroyDescriptorPool(DEVICE, oldDescriptorPool, null);
                });
            }

            this.descriptorPool = pDescriptorPool.get(0);
        }

        public void resetIdx() {
            this.currentIdx = -1;
            this.writtenCount = 0;
        }

        private void cleanUp() {
            vkResetDescriptorPool(DEVICE, descriptorPool, 0);
            vkDestroyDescriptorPool(DEVICE, descriptorPool, null);

            MemoryUtil.memFree(this.dynamicOffsets);
        }

    }

    public static class Builder {

        public static GraphicsPipeline createGraphicsPipeline(VertexFormat format, String path) {
            Pipeline.Builder pipelineBuilder = new Pipeline.Builder(format, path);
            pipelineBuilder.parseBindingsJSON();
            pipelineBuilder.compileShaders();
            return pipelineBuilder.createGraphicsPipeline();
        }

        final VertexFormat vertexFormat;
        VertexFormat instanceFormat;
        final String shaderPath;
        List<UBO> UBOs;
        ManualUBO manualUBO;
        PushConstants pushConstants;
        List<ImageDescriptor> imageDescriptors;
        int nextBinding;

        SPIRV vertShaderSPIRV;
        SPIRV fragShaderSPIRV;

        RenderPass renderPass;

        public Builder(VertexFormat vertexFormat, String path) {
            this.vertexFormat = vertexFormat;
            this.shaderPath = path;
        }

        public Builder(VertexFormat vertexFormat) {
            this(vertexFormat, null);
        }

        public void setInstanceFormat(VertexFormat instanceFormat) {
            this.instanceFormat = instanceFormat;
        }

        public GraphicsPipeline createGraphicsPipeline() {
            Validate.isTrue(this.imageDescriptors != null && this.UBOs != null
                            && this.vertShaderSPIRV != null && this.fragShaderSPIRV != null,
                    "Cannot create Pipeline: resources missing");

            if (this.manualUBO != null)
                this.UBOs.add(this.manualUBO);

            return new GraphicsPipeline(this);
        }

        public void setUniforms(List<UBO> UBOs, List<ImageDescriptor> imageDescriptors) {
            this.UBOs = UBOs;
            this.imageDescriptors = imageDescriptors;
        }

        public void setSPIRVs(SPIRV vertShaderSPIRV, SPIRV fragShaderSPIRV) {
            this.vertShaderSPIRV = vertShaderSPIRV;
            this.fragShaderSPIRV = fragShaderSPIRV;
        }

        public void compileShaders() {
            // union fs drops slash
            String resourcePath = SPIRVUtils.class.getResource("/assets/nyalkan/shaders/").toExternalForm();
            if (!resourcePath.endsWith("/")) {
                resourcePath += "/";
            }

            this.vertShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.vsh", resourcePath, this.shaderPath), ShaderKind.VERTEX_SHADER);
            this.fragShaderSPIRV = compileShaderAbsoluteFile(String.format("%s%s.fsh", resourcePath, this.shaderPath), ShaderKind.FRAGMENT_SHADER);
        }

        public void compileShaders(String name, String vsh, String fsh) {
            this.vertShaderSPIRV = compileShader(String.format("%s.vsh", name), vsh, ShaderKind.VERTEX_SHADER);
            this.fragShaderSPIRV = compileShader(String.format("%s.fsh", name), fsh, ShaderKind.FRAGMENT_SHADER);
        }

        public void parseBindingsJSON() {
            Validate.notNull(this.shaderPath, "Cannot parse bindings: shaderPath is null");

            this.UBOs = new ArrayList<>();
            this.imageDescriptors = new ArrayList<>();

            JsonObject jsonObject;

            String resourcePath = String.format("/assets/nyalkan/shaders/%s.json", this.shaderPath);
            InputStream stream = Pipeline.class.getResourceAsStream(resourcePath);

            if (stream == null)
                throw new NullPointerException(String.format("Failed to load: %s", resourcePath));

            jsonObject = GsonHelper.parse(new InputStreamReader(stream, StandardCharsets.UTF_8));

            JsonArray jsonUbos = GsonHelper.getAsJsonArray(jsonObject, "UBOs", null);
            JsonArray jsonManualUbos = GsonHelper.getAsJsonArray(jsonObject, "ManualUBOs", null);
            JsonArray jsonSamplers = GsonHelper.getAsJsonArray(jsonObject, "samplers", null);
            JsonArray jsonPushConstants = GsonHelper.getAsJsonArray(jsonObject, "PushConstants", null);

            if (jsonUbos != null) {
                for (JsonElement jsonelement : jsonUbos) {
                    this.parseUboNode(jsonelement);
                }
            }

            if (jsonManualUbos != null) {
                this.parseManualUboNode(jsonManualUbos.get(0));
            }

            if (jsonSamplers != null) {
                for (JsonElement jsonelement : jsonSamplers) {
                    this.parseSamplerNode(jsonelement);
                }
            }

            if (jsonPushConstants != null) {
                this.parsePushConstantNode(jsonPushConstants);
            }
        }

        private void parseUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "UBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int type = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            JsonArray fields = GsonHelper.getAsJsonArray(jsonobject, "fields");

            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for (JsonElement jsonelement2 : fields) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement2, "uniform");
                //need to store some infos
                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addUniformInfo(type2, name, j);

            }
            UBO ubo = builder.buildUBO(binding, type);

            if (binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.UBOs.add(ubo);
        }

        private void parseManualUboNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "ManualUBO");
            int binding = GsonHelper.getAsInt(jsonobject, "binding");
            int stage = getStageFromString(GsonHelper.getAsString(jsonobject, "type"));
            int size = GsonHelper.getAsInt(jsonobject, "size");

            if (binding >= this.nextBinding)
                this.nextBinding = binding + 1;

            this.manualUBO = new ManualUBO(binding, stage, size);
        }

        private void parseSamplerNode(JsonElement jsonelement) {
            JsonObject jsonobject = GsonHelper.convertToJsonObject(jsonelement, "Sampler");
            String name = GsonHelper.getAsString(jsonobject, "name");

            int imageIdx = VTextureSelector.getTextureIdx(name);
            this.imageDescriptors.add(new ImageDescriptor(this.nextBinding, "sampler2D", name, imageIdx));
            this.nextBinding++;
        }

        private void parsePushConstantNode(JsonArray jsonArray) {
            AlignedStruct.Builder builder = new AlignedStruct.Builder();

            for (JsonElement jsonelement : jsonArray) {
                JsonObject jsonobject2 = GsonHelper.convertToJsonObject(jsonelement, "PC");

                String name = GsonHelper.getAsString(jsonobject2, "name");
                String type2 = GsonHelper.getAsString(jsonobject2, "type");
                int j = GsonHelper.getAsInt(jsonobject2, "count");

                builder.addUniformInfo(type2, name, j);
            }

            this.pushConstants = builder.buildPushConstant();
        }

        public static int getStageFromString(String s) {
            return switch (s) {
                case "vertex" -> VK_SHADER_STAGE_VERTEX_BIT;
                case "fragment" -> VK_SHADER_STAGE_FRAGMENT_BIT;
                case "all" -> VK_SHADER_STAGE_ALL_GRAPHICS;
                case "compute" -> VK_SHADER_STAGE_COMPUTE_BIT;

                default -> throw new RuntimeException("cannot identify type..");
            };
        }
    }
}
