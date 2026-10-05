package net.nyalkan.vulkan;

import net.nyalkan.render.immediate.ImmediateBatcher;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Minecraft;
import net.nyalkan.Initializer;
import net.nyalkan.gl.GlFramebuffer;
import net.nyalkan.mixin.window.WindowAccessor;
import net.nyalkan.render.PipelineManager;
import net.nyalkan.render.chunk.WorldRenderer;
import net.nyalkan.render.chunk.buffer.UploadManager;
import net.nyalkan.render.profiling.Profiler;
import net.nyalkan.vulkan.device.DeviceManager;
import net.nyalkan.vulkan.framebuffer.Framebuffer;
import net.nyalkan.vulkan.framebuffer.RenderPass;
import net.nyalkan.vulkan.memory.MemoryManager;
import net.nyalkan.vulkan.pass.DefaultMainPass;
import net.nyalkan.vulkan.pass.MainPass;
import net.nyalkan.vulkan.shader.GraphicsPipeline;
import net.nyalkan.vulkan.shader.Pipeline;
import net.nyalkan.vulkan.shader.PipelineState;
import net.nyalkan.vulkan.shader.Uniforms;
import net.nyalkan.vulkan.shader.layout.PushConstants;
import net.nyalkan.vulkan.texture.VTextureSelector;
import net.nyalkan.vulkan.texture.VulkanImage;
import net.nyalkan.vulkan.util.VUtil;
import net.nyalkan.vulkan.util.VkResult;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static com.mojang.blaze3d.platform.GlConst.GL_COLOR_BUFFER_BIT;
import static com.mojang.blaze3d.platform.GlConst.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_STENCIL_BUFFER_BIT;
import static net.nyalkan.vulkan.Vulkan.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class Renderer {
    private static Renderer INSTANCE;

    private static VkDevice device;

    private static boolean swapChainUpdate = false;
    public static boolean skipRendering = false;

    public static void initRenderer() {
        INSTANCE = new Renderer();
        INSTANCE.init();
    }

    public static Renderer getInstance() {
        return INSTANCE;
    }

    public static Drawer getDrawer() {
        return INSTANCE.drawer;
    }

    public static int getCurrentFrame() {
        return currentFrame;
    }

    public static int getCurrentImage() {
        return imageIndex;
    }

    private final Set<Pipeline> usedPipelines = new ObjectOpenHashSet<>();
    private Pipeline boundPipeline;
    private long boundPipelineHandle;

    private Drawer drawer;

    private int framesNum;
    private int imagesNum;
    private List<VkCommandBuffer> commandBuffers;
    private ArrayList<Long> imageAvailableSemaphores;
    private ArrayList<Long> renderFinishedSemaphores;
    private ArrayList<Long> inFlightFences;

    private Framebuffer boundFramebuffer;
    private RenderPass boundRenderPass;

    private List<VkCommandBuffer> hudCommandBuffers;
    private boolean hudBegun;
    private VkCommandBuffer mainCmdBuffer;
    private final CmdState mainState = new CmdState();
    private final CmdState hudState = new CmdState();
    private GlFramebuffer.Binding mainBinding;

    private static final class CmdState {
        Framebuffer framebuffer;
        RenderPass renderPass;
        Pipeline pipeline;
        long pipelineHandle;
        int stencilRef, stencilCompareMask, stencilWriteMask;
        final float[] blendColor = new float[4];
        float depthBiasUnits, depthBiasFactor;
        boolean invertedViewport;
        int labels;
    }

    private static int currentFrame = 0;
    private static int imageIndex;
    private static int lastReset = -1;
    private VkCommandBuffer currentCmdBuffer;
    private boolean recordingCmds = false;

    MainPass mainPass = DefaultMainPass.create();

    private final List<Runnable> onResizeCallbacks = new ObjectArrayList<>();

    public Renderer() {
        device = Vulkan.getVkDevice();
        framesNum = Initializer.CONFIG.frameQueueSize;
        imagesNum = getSwapChain().getImagesNum();
    }

    public static void setLineWidth(float width) {
        if (INSTANCE.boundFramebuffer == null) {
            return;
        }
        vkCmdSetLineWidth(INSTANCE.currentCmdBuffer, width);
    }

    private void init() {
        MemoryManager.createInstance(Renderer.getFramesNum());
        Vulkan.createStagingBuffers();

        drawer = new Drawer();
        drawer.createResources(framesNum);

        Uniforms.setupDefaultUniforms();
        PipelineManager.init();
        UploadManager.createInstance();

        allocateCommandBuffers();
        createSyncObjects();
    }

    private void allocateCommandBuffers() {
        if (commandBuffers != null) {
            commandBuffers.forEach(commandBuffer -> vkFreeCommandBuffers(device, Vulkan.getCommandPool(), commandBuffer));
        }
        if (hudCommandBuffers != null) {
            hudCommandBuffers.forEach(commandBuffer -> vkFreeCommandBuffers(device, Vulkan.getCommandPool(), commandBuffer));
        }

        commandBuffers = new ArrayList<>(framesNum);

        try (MemoryStack stack = stackPush()) {

            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.calloc(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(getCommandPool());
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(framesNum);

            PointerBuffer pCommandBuffers = stack.mallocPointer(framesNum);

            int vkResult = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers);
            if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers: %s".formatted(VkResult.decode(vkResult)));
            }

            for (int i = 0; i < framesNum; i++) {
                commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
            }

            vkResult = vkAllocateCommandBuffers(device, allocInfo, pCommandBuffers);
            if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to allocate command buffers: %s".formatted(VkResult.decode(vkResult)));
            }

            hudCommandBuffers = new ArrayList<>(framesNum);
            for (int i = 0; i < framesNum; i++) {
                hudCommandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), device));
            }
            hudBegun = false;
        }
    }

    private void createSyncObjects() {
        imageAvailableSemaphores = new ArrayList<>(framesNum);
        renderFinishedSemaphores = new ArrayList<>(framesNum);
        inFlightFences = new ArrayList<>(framesNum);

        try (MemoryStack stack = stackPush()) {

            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < framesNum; i++) {

                if (vkCreateSemaphore(device, semaphoreInfo, null, pImageAvailableSemaphore) != VK_SUCCESS
                        || vkCreateSemaphore(device, semaphoreInfo, null, pRenderFinishedSemaphore) != VK_SUCCESS
                        || vkCreateFence(device, fenceInfo, null, pFence) != VK_SUCCESS) {

                    throw new RuntimeException("Failed to create synchronization objects for the frame: " + i);
                }

                imageAvailableSemaphores.add(pImageAvailableSemaphore.get(0));
                renderFinishedSemaphores.add(pRenderFinishedSemaphore.get(0));
                inFlightFences.add(pFence.get(0));

            }

        }
    }

    public void beginFrame() {
        Profiler p = Profiler.getMainProfiler();
        p.pop();
        p.push("Frame_fence");

        if (swapChainUpdate) {
            recreateSwapChain();
            swapChainUpdate = false;

            if (getSwapChain().getWidth() == 0 && getSwapChain().getHeight() == 0) {
                skipRendering = true;
                Minecraft.getInstance().noRender = true;
            } else {
                skipRendering = false;
                Minecraft.getInstance().noRender = false;
            }
        }


        if (skipRendering || recordingCmds)
            return;

        vkWaitForFences(device, inFlightFences.get(currentFrame), true, VUtil.UINT64_MAX);

        p.pop();
        p.push("Begin_rendering");

        MemoryManager.getInstance().initFrame(currentFrame);
        drawer.setCurrentFrame(currentFrame);

        resetDescriptors();

        currentCmdBuffer = commandBuffers.get(currentFrame);
        vkResetCommandBuffer(currentCmdBuffer, 0);
        hudBegun = false;
        mainCmdBuffer = null;

        try (MemoryStack stack = stackPush()) {

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(device, Vulkan.getSwapChain().getId(), VUtil.UINT64_MAX,
                    imageAvailableSemaphores.get(currentFrame), VK_NULL_HANDLE, pImageIndex);

            if (vkResult == VK_SUBOPTIMAL_KHR || vkResult == VK_ERROR_OUT_OF_DATE_KHR || swapChainUpdate) {
                swapChainUpdate = true;
                skipRendering = true;
                beginFrame();

                return;
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Cannot acquire next swap chain image: %s".formatted(VkResult.decode(vkResult)));
            }

            imageIndex = pImageIndex.get(0);

            this.beginRenderPass(stack);
        }

        p.pop();
    }

    private void beginRenderPass(MemoryStack stack) {
        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
        beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
        beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        VkCommandBuffer commandBuffer = currentCmdBuffer;

        int vkResult = vkBeginCommandBuffer(commandBuffer, beginInfo);
        if (vkResult != VK_SUCCESS) {
            throw new RuntimeException("Failed to begin recording command buffer: %s".formatted(VkResult.decode(vkResult)));
        }

        recordingCmds = true;
        drawer.invalidateBindings();
        Pipeline.invalidateBoundSets();
        openLabels = 0;
        labelEpoch++;
        mainPass.begin(commandBuffer, stack);

        resetDynamicState(commandBuffer);
    }

    private static long frameCount;

    public static long getFrameCount() {
        return frameCount;
    }

    public void endFrame() {
        frameCount++;

        if (skipRendering || !recordingCmds)
            return;

        // pending draws before submit
        ImmediateBatcher.INSTANCE.flush();

        Profiler p = Profiler.getMainProfiler();
        p.push("End_rendering");

        endHudRecording(null);
        closeDebugSections(currentCmdBuffer);
        mainPass.end(currentCmdBuffer);

        submitFrame();
        recordingCmds = false;

        p.pop();
        p.push("Post_rendering");
    }

    private void submitFrame() {
        if (swapChainUpdate)
            return;

        try (MemoryStack stack = stackPush()) {
            int vkResult;

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(stack.longs(imageAvailableSemaphores.get(currentFrame)));
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(stack.longs(renderFinishedSemaphores.get(currentFrame)));

            VkCommandBuffer hud = finishHudBuffer();
            submitInfo.pCommandBuffers(hud != null ? stack.pointers(hud, currentCmdBuffer) : stack.pointers(currentCmdBuffer));

            vkResetFences(device, inFlightFences.get(currentFrame));

            Synchronization.INSTANCE.waitFences();

            if ((vkResult = vkQueueSubmit(DeviceManager.getGraphicsQueue().queue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, inFlightFences.get(currentFrame));
                throw new RuntimeException("Failed to submit draw command buffer: %s".formatted(VkResult.decode(vkResult)));
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(stack.longs(renderFinishedSemaphores.get(currentFrame)));

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(Vulkan.getSwapChain().getId()));

            presentInfo.pImageIndices(stack.ints(imageIndex));

            vkResult = vkQueuePresentKHR(DeviceManager.getPresentQueue().queue(), presentInfo);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || swapChainUpdate) {
                swapChainUpdate = true;
                return;
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present rendered frame: %s".formatted(VkResult.decode(vkResult)));
            }

            currentFrame = (currentFrame + 1) % framesNum;
        }
    }

    /**
    * Called in case draw results are needed before the of the frame
     */
    public void flushCmds() {
        if (!this.recordingCmds)
            return;

        try (MemoryStack stack = stackPush()) {
            int vkResult;

            endHudRecording(null);
            this.endRenderPass(currentCmdBuffer);
            closeDebugSections(currentCmdBuffer);
            vkEndCommandBuffer(currentCmdBuffer);

            VkSubmitInfo submitInfo = VkSubmitInfo.calloc(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            VkCommandBuffer hud = finishHudBuffer();
            submitInfo.pCommandBuffers(hud != null ? stack.pointers(hud, currentCmdBuffer) : stack.pointers(currentCmdBuffer));

            vkResetFences(device, inFlightFences.get(currentFrame));

            Synchronization.INSTANCE.waitFences();

            if ((vkResult = vkQueueSubmit(DeviceManager.getGraphicsQueue().queue(), submitInfo, inFlightFences.get(currentFrame))) != VK_SUCCESS) {
                vkResetFences(device, inFlightFences.get(currentFrame));
                throw new RuntimeException("Failed to submit draw command buffer: %s".formatted(VkResult.decode(vkResult)));
            }

            vkWaitForFences(device, inFlightFences.get(currentFrame), true, VUtil.UINT64_MAX);

            this.beginRenderPass(stack);
        }
    }

    public void endRenderPass() {
        endRenderPass(currentCmdBuffer);
    }

    public void endRenderPass(VkCommandBuffer commandBuffer) {
        if (skipRendering || this.boundFramebuffer == null)
            return;

        if (!DYNAMIC_RENDERING)
            this.boundRenderPass.endRenderPass(currentCmdBuffer);
        else
            KHRDynamicRendering.vkCmdEndRenderingKHR(commandBuffer);

        this.boundRenderPass = null;
        this.boundFramebuffer = null;

        GlFramebuffer.resetBoundFramebuffer();
    }

    public boolean beginRendering(RenderPass renderPass, Framebuffer framebuffer) {
        if (skipRendering || !recordingCmds)
            return false;

        if (this.boundFramebuffer != framebuffer) {
            this.endRenderPass(currentCmdBuffer);

            try (MemoryStack stack = stackPush()) {
                framebuffer.beginRenderPass(currentCmdBuffer, renderPass, stack);
            }

            this.boundFramebuffer = framebuffer;
        }
        return true;
    }

    public void preInitFrame() {
        Profiler p = Profiler.getMainProfiler();
        p.pop();
        p.round();
        p.push("Frame_ops");

        // runTick might be called recursively,
        // this check forces sync to avoid upload corruption
        if (lastReset == currentFrame) {
            Synchronization.INSTANCE.waitFences();
        }
        lastReset = currentFrame;

        drawer.resetBuffers(currentFrame);

        Vulkan.getStagingBuffer().reset();

        WorldRenderer.getInstance().uploadSections();
        UploadManager.INSTANCE.submitUploads();
    }

    public void addUsedPipeline(Pipeline pipeline) {
        usedPipelines.add(pipeline);
    }

    public void removeUsedPipeline(Pipeline pipeline) {
        usedPipelines.remove(pipeline);
    }

    private void resetDescriptors() {
        for (Pipeline pipeline : usedPipelines) {
            pipeline.resetDescriptorPool(currentFrame);
        }

        usedPipelines.clear();
        boundPipeline = null;
        boundPipelineHandle = 0;
    }

    void waitForSwapChain() {
        vkResetFences(device, inFlightFences.get(currentFrame));

//        constexpr VkPipelineStageFlags t=VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            //Empty Submit
            VkSubmitInfo info = VkSubmitInfo.calloc(stack)
                    .sType$Default()
                    .pWaitSemaphores(stack.longs(imageAvailableSemaphores.get(currentFrame)))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_ALL_COMMANDS_BIT));

            vkQueueSubmit(DeviceManager.getGraphicsQueue().queue(), info, inFlightFences.get(currentFrame));
            vkWaitForFences(device, inFlightFences.get(currentFrame), true, -1);
        }
    }

    @SuppressWarnings("UnreachableCode")
    private void recreateSwapChain() {
        Synchronization.INSTANCE.waitFences();
        Vulkan.waitIdle();
        MemoryManager.setImmediateFree(true);
        try {
            this.recreateSwapChainIdle();
        } finally {
            MemoryManager.setImmediateFree(false);
        }
    }

    private void recreateSwapChainIdle() {

        commandBuffers.forEach(commandBuffer -> vkResetCommandBuffer(commandBuffer, 0));
        hudCommandBuffers.forEach(commandBuffer -> vkResetCommandBuffer(commandBuffer, 0));
        hudBegun = false;
        mainCmdBuffer = null;

        Vulkan.getSwapChain().recreate();

        //Semaphores need to be recreated in order to make them unsignaled
        destroySyncObjects();

        int newFramesNum = Initializer.CONFIG.frameQueueSize;
        imagesNum = getSwapChain().getImagesNum();

        if (framesNum != newFramesNum) {
            UploadManager.INSTANCE.submitUploads();

            framesNum = newFramesNum;
            MemoryManager.createInstance(newFramesNum);
            createStagingBuffers();
            allocateCommandBuffers();

            Pipeline.recreateDescriptorSets(framesNum);

            drawer.createResources(framesNum);
        }

        createSyncObjects();

        this.onResizeCallbacks.forEach(Runnable::run);
        ((WindowAccessor) (Object) Minecraft.getInstance().getWindow()).getEventHandler().resizeDisplay();

        currentFrame = 0;
    }

    public void cleanUpResources() {
        destroySyncObjects();

        drawer.cleanUpResources();

        PipelineManager.destroyPipelines();
        VTextureSelector.getWhiteTexture().free();
    }

    private void destroySyncObjects() {
        for (int i = 0; i < framesNum; ++i) {
            vkDestroyFence(device, inFlightFences.get(i), null);
            vkDestroySemaphore(device, imageAvailableSemaphores.get(i), null);
            vkDestroySemaphore(device, renderFinishedSemaphores.get(i), null);
        }
    }

    public Framebuffer getBoundFramebuffer() {
        return this.boundFramebuffer;
    }

    public void setBoundFramebuffer(Framebuffer framebuffer) {
        this.boundFramebuffer = framebuffer;
    }

    public void setBoundRenderPass(RenderPass boundRenderPass) {
        this.boundRenderPass = boundRenderPass;
    }

    public RenderPass getBoundRenderPass() {
        return boundRenderPass;
    }

    public void setMainPass(MainPass mainPass) {
        this.mainPass = mainPass;
    }

    public MainPass getMainPass() {
        return this.mainPass;
    }

    public void addOnResizeCallback(Runnable runnable) {
        this.onResizeCallbacks.add(runnable);
    }

    public void bindGraphicsPipeline(GraphicsPipeline pipeline) {
        bindGraphicsPipeline(pipeline, null);
    }

    // optional buffer format
    public void bindGraphicsPipeline(GraphicsPipeline pipeline, VertexFormat bufferFormat) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        // lazy main target bind
        if (boundRenderPass == null) {
            getMainPass().rebindMainTarget();
        }

        PipelineState currentState = PipelineState.getCurrentPipelineState(boundRenderPass);
        final long handle = bufferFormat != null ? pipeline.getHandle(currentState, bufferFormat) : pipeline.getHandle(currentState);

        if (recordedStencilRef != VRenderSystem.stencilRef) {
            vkCmdSetStencilReference(commandBuffer, VK_STENCIL_FACE_FRONT_AND_BACK, VRenderSystem.stencilRef);
            recordedStencilRef = VRenderSystem.stencilRef;
        }
        if (recordedStencilCompareMask != VRenderSystem.stencilFuncMask) {
            vkCmdSetStencilCompareMask(commandBuffer, VK_STENCIL_FACE_FRONT_AND_BACK, VRenderSystem.stencilFuncMask);
            recordedStencilCompareMask = VRenderSystem.stencilFuncMask;
        }
        if (recordedStencilWriteMask != VRenderSystem.stencilWriteMask) {
            vkCmdSetStencilWriteMask(commandBuffer, VK_STENCIL_FACE_FRONT_AND_BACK, VRenderSystem.stencilWriteMask);
            recordedStencilWriteMask = VRenderSystem.stencilWriteMask;
        }
        if (!Arrays.equals(recordedBlendColor, VRenderSystem.blendColor)) {
            vkCmdSetBlendConstants(commandBuffer, VRenderSystem.blendColor);
            System.arraycopy(VRenderSystem.blendColor, 0, recordedBlendColor, 0, 4);
        }

        if (boundPipelineHandle == handle) {
            return;
        }

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, handle);
        boundPipelineHandle = handle;
        boundPipeline = pipeline;
        addUsedPipeline(pipeline);
    }

    public void uploadAndBindUBOs(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;
        pipeline.bindDescriptorSets(commandBuffer, currentFrame);
    }

    public void pushConstants(Pipeline pipeline) {
        VkCommandBuffer commandBuffer = currentCmdBuffer;

        PushConstants pushConstants = pipeline.getPushConstants();

        try (MemoryStack stack = stackPush()) {
            ByteBuffer buffer = stack.malloc(pushConstants.getSize());
            long ptr = MemoryUtil.memAddress0(buffer);
            pushConstants.update(ptr);

            nvkCmdPushConstants(commandBuffer, pipeline.getLayout(), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants.getSize(), ptr);
        }

    }

    public Pipeline getBoundPipeline() {
        return boundPipeline;
    }

    // values recorded in currentCmdBuffer
    private static int recordedStencilRef, recordedStencilCompareMask, recordedStencilWriteMask;
    private static final float[] recordedBlendColor = new float[4];

    private static void resetDynamicState(VkCommandBuffer commandBuffer) {
        vkCmdSetDepthBias(commandBuffer, 0.0F, 0.0F, 0.0F);
        depthBiasUnits = 0.0F;
        depthBiasFactor = 0.0F;

        vkCmdSetLineWidth(commandBuffer, 1.0F);

        vkCmdSetStencilReference(commandBuffer, VK_STENCIL_FACE_FRONT_AND_BACK, 0);
        vkCmdSetStencilCompareMask(commandBuffer, VK_STENCIL_FACE_FRONT_AND_BACK, 0xFF);
        vkCmdSetStencilWriteMask(commandBuffer, VK_STENCIL_FACE_FRONT_AND_BACK, 0xFF);
        vkCmdSetBlendConstants(commandBuffer, new float[]{0.0f, 0.0f, 0.0f, 0.0f});

        recordedStencilRef = 0;
        recordedStencilCompareMask = 0xFF;
        recordedStencilWriteMask = 0xFF;
        Arrays.fill(recordedBlendColor, 0.0f);
    }

    // last applied depth bias
    private static float depthBiasUnits, depthBiasFactor;

    public static void setDepthBias(float units, float factor) {
        VkCommandBuffer commandBuffer = INSTANCE.currentCmdBuffer;

        vkCmdSetDepthBias(commandBuffer, units, 0.0f, factor);
        depthBiasUnits = units;
        depthBiasFactor = factor;
    }

    public static float getDepthBiasUnits() {
        return depthBiasUnits;
    }

    public static float getDepthBiasFactor() {
        return depthBiasFactor;
    }

    public static void clearAttachments(int v) {
        Framebuffer framebuffer = Renderer.getInstance().boundFramebuffer;
        if (framebuffer == null)
            return;

        clearAttachments(v, framebuffer.getWidth(), framebuffer.getHeight());
    }

    public static void clearAttachments(int v, int width, int height) {
        if (skipRendering)
            return;

        try (MemoryStack stack = stackPush()) {
            //ClearValues have to be different for each attachment to clear,
            //it seems it uses the same buffer: color and depth values override themselves
            VkClearValue colorValue = VkClearValue.calloc(stack);
            colorValue.color().float32(VRenderSystem.clearColor);

            VkClearValue depthValue = VkClearValue.calloc(stack);
            depthValue.depthStencil().set(VRenderSystem.clearDepthValue, 0); //Use fast depth clears if possible

            // only requested attachments
            boolean clearColorBit = (v & GL_COLOR_BUFFER_BIT) != 0;
            boolean clearDepthOrStencilBit = (v & (GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT)) != 0;

            int attachmentsCount = (clearColorBit ? 1 : 0) + (clearDepthOrStencilBit ? 1 : 0);
            if (attachmentsCount == 0)
                return;

            final VkClearAttachment.Buffer pAttachments = VkClearAttachment.malloc(attachmentsCount, stack);
            int idx = 0;

            if (clearColorBit) {
                VkClearAttachment clearColor = pAttachments.get(idx++);
                clearColor.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                clearColor.colorAttachment(0);
                clearColor.clearValue(colorValue);
            }

            if (clearDepthOrStencilBit) {
                int aspectMask = 0;
                if ((v & GL_DEPTH_BUFFER_BIT) != 0) aspectMask |= VK_IMAGE_ASPECT_DEPTH_BIT;
                if ((v & GL_STENCIL_BUFFER_BIT) != 0) aspectMask |= VK_IMAGE_ASPECT_STENCIL_BIT;

                VkClearAttachment clearDepth = pAttachments.get(idx);
                clearDepth.aspectMask(aspectMask);
                clearDepth.colorAttachment(0);
                clearDepth.clearValue(depthValue);
            }

            //Rect to clear
            VkRect2D renderArea = VkRect2D.malloc(stack);
            renderArea.offset().set(0, 0);
            renderArea.extent().set(width, height);

            VkClearRect.Buffer pRect = VkClearRect.malloc(1, stack);
            pRect.rect(renderArea);
            pRect.baseArrayLayer(0);
            pRect.layerCount(1);

            vkCmdClearAttachments(INSTANCE.currentCmdBuffer, pAttachments, pRect);
        }
    }

    public static void setInvertedViewport(int x, int y, int width, int height) {
        setViewport(x, y + height, width, -height);
        VRenderSystem.invertedViewport = true;
    }

    public static void setViewport(int x, int y, int width, int height) {
        VRenderSystem.invertedViewport = false;
        try (MemoryStack stack = stackPush()) {
            setViewport(x, y, width, height, stack);
        }
    }

    public static void setViewport(int x, int y, int width, int height, MemoryStack stack) {
        if (!INSTANCE.recordingCmds)
            return;

        VkViewport.Buffer viewport = VkViewport.malloc(1, stack);
        viewport.x(x);
        viewport.y(height + y);
        viewport.width(width);
        viewport.height(-height);
        viewport.minDepth(0.0f);
        viewport.maxDepth(1.0f);

        vkCmdSetViewport(INSTANCE.currentCmdBuffer, 0, viewport);
    }

    public static void resetViewport() {
        VRenderSystem.invertedViewport = false;
        try (MemoryStack stack = stackPush()) {
            int width = getSwapChain().getWidth();
            int height = getSwapChain().getHeight();

            VkViewport.Buffer viewport = VkViewport.malloc(1, stack);
            viewport.x(0.0f);
            viewport.y(height);
            viewport.width(width);
            viewport.height(-height);
            viewport.minDepth(0.0f);
            viewport.maxDepth(1.0f);

            vkCmdSetViewport(INSTANCE.currentCmdBuffer, 0, viewport);
        }
    }

    public static void setScissor(int x, int y, int width, int height) {
        if (INSTANCE.boundFramebuffer == null)
            return;

        try (MemoryStack stack = stackPush()) {
            int framebufferHeight = INSTANCE.boundFramebuffer.getHeight();

            x = Math.max(0, x);

            VkRect2D.Buffer scissor = VkRect2D.malloc(1, stack);
            scissor.offset().set(x, framebufferHeight - (y + height));
            scissor.extent().set(width, height);

            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    public static void resetScissor() {
        if (INSTANCE.boundFramebuffer == null)
            return;

        try (MemoryStack stack = stackPush()) {
            VkRect2D.Buffer scissor = INSTANCE.boundFramebuffer.scissor(stack);
            vkCmdSetScissor(INSTANCE.currentCmdBuffer, 0, scissor);
        }
    }

    private static int openLabels;
    private static int labelEpoch;

    public static int getLabelEpoch() {
        return labelEpoch;
    }

    public static boolean pushDebugSection(String s) {
        if (!Vulkan.DEBUG_UTILS || INSTANCE == null || !INSTANCE.recordingCmds)
            return false;

        try (MemoryStack stack = stackPush()) {
            VkDebugUtilsLabelEXT markerInfo = VkDebugUtilsLabelEXT.calloc(stack);
            markerInfo.sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_LABEL_EXT);
            markerInfo.pLabelName(stack.UTF8(s));
            vkCmdBeginDebugUtilsLabelEXT(INSTANCE.currentCmdBuffer, markerInfo);
        }
        openLabels++;
        return true;
    }

    public static void popDebugSection() {
        if (!Vulkan.DEBUG_UTILS || INSTANCE == null || !INSTANCE.recordingCmds || openLabels == 0)
            return;

        vkCmdEndDebugUtilsLabelEXT(INSTANCE.currentCmdBuffer);
        openLabels--;
    }

    public static void popPushDebugSection(String s) {
        popDebugSection();
        pushDebugSection(s);
    }

    public static void closeDebugSections(VkCommandBuffer commandBuffer) {
        if (!Vulkan.DEBUG_UTILS)
            return;

        for (; openLabels > 0; openLabels--)
            vkCmdEndDebugUtilsLabelEXT(commandBuffer);
        labelEpoch++;
    }

    public boolean isRecordingHud() {
        return this.mainCmdBuffer != null;
    }

    public boolean beginHudRecording() {
        if (!this.recordingCmds || skipRendering || this.mainCmdBuffer != null)
            return false;

        this.saveState(this.mainState);
        this.mainBinding = GlFramebuffer.saveBinding();
        this.mainCmdBuffer = this.currentCmdBuffer;

        VkCommandBuffer hud = this.hudCommandBuffers.get(currentFrame);
        this.currentCmdBuffer = hud;
        GlFramebuffer.resetBoundFramebuffer();

        if (!this.hudBegun) {
            vkResetCommandBuffer(hud, 0);
            try (MemoryStack stack = stackPush()) {
                VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc(stack);
                beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
                beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
                vkBeginCommandBuffer(hud, beginInfo);
            }
            this.hudBegun = true;

            this.boundFramebuffer = null;
            this.boundRenderPass = null;
            this.boundPipeline = null;
            this.boundPipelineHandle = 0;
            openLabels = 0;
            VRenderSystem.invertedViewport = false;
            resetDynamicState(hud);
        } else {
            this.loadState(this.hudState);
        }
        return true;
    }

    public void endHudRecording(VulkanImage[] readImages) {
        if (this.mainCmdBuffer == null)
            return;

        this.endRenderPass(this.currentCmdBuffer);
        if (readImages != null) {
            try (MemoryStack stack = stackPush()) {
                for (VulkanImage image : readImages)
                    image.readOnlyLayout(stack, this.currentCmdBuffer);
            }
        }

        this.saveState(this.hudState);
        this.currentCmdBuffer = this.mainCmdBuffer;
        this.mainCmdBuffer = null;
        this.loadState(this.mainState);
        GlFramebuffer.restoreBinding(this.mainBinding);
    }

    private VkCommandBuffer finishHudBuffer() {
        if (!this.hudBegun)
            return null;

        VkCommandBuffer hud = this.hudCommandBuffers.get(currentFrame);
        if (Vulkan.DEBUG_UTILS) {
            int labels = openLabels;
            openLabels = this.hudState.labels;
            closeDebugSections(hud);
            openLabels = labels;
        }
        vkEndCommandBuffer(hud);
        this.hudBegun = false;
        return hud;
    }

    private void saveState(CmdState state) {
        state.framebuffer = this.boundFramebuffer;
        state.renderPass = this.boundRenderPass;
        state.pipeline = this.boundPipeline;
        state.pipelineHandle = this.boundPipelineHandle;
        state.stencilRef = recordedStencilRef;
        state.stencilCompareMask = recordedStencilCompareMask;
        state.stencilWriteMask = recordedStencilWriteMask;
        System.arraycopy(recordedBlendColor, 0, state.blendColor, 0, 4);
        state.depthBiasUnits = depthBiasUnits;
        state.depthBiasFactor = depthBiasFactor;
        state.invertedViewport = VRenderSystem.invertedViewport;
        state.labels = openLabels;
    }

    private void loadState(CmdState state) {
        this.boundFramebuffer = state.framebuffer;
        this.boundRenderPass = state.renderPass;
        this.boundPipeline = state.pipeline;
        this.boundPipelineHandle = state.pipelineHandle;
        recordedStencilRef = state.stencilRef;
        recordedStencilCompareMask = state.stencilCompareMask;
        recordedStencilWriteMask = state.stencilWriteMask;
        System.arraycopy(state.blendColor, 0, recordedBlendColor, 0, 4);
        depthBiasUnits = state.depthBiasUnits;
        depthBiasFactor = state.depthBiasFactor;
        VRenderSystem.invertedViewport = state.invertedViewport;
        openLabels = state.labels;
    }

    public static int getFramesNum() {
        return INSTANCE.framesNum;
    }

    public static VkCommandBuffer getCommandBuffer() {
        return INSTANCE.currentCmdBuffer;
    }

    public static boolean isRecording() {
        return INSTANCE.recordingCmds;
    }

    public static void scheduleSwapChainUpdate() {
        swapChainUpdate = true;
    }
}
