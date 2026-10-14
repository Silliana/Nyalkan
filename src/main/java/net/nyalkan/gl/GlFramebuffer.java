package net.nyalkan.gl;

import net.nyalkan.render.immediate.ImmediateBatcher;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import it.unimi.dsi.fastutil.ints.Int2ReferenceOpenHashMap;
import net.minecraft.client.Minecraft;
import net.nyalkan.render.gui.HudCache;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.Vulkan;
import net.nyalkan.vulkan.framebuffer.Framebuffer;
import net.nyalkan.vulkan.framebuffer.RenderPass;
import net.nyalkan.vulkan.texture.VTextureSelector;
import net.nyalkan.vulkan.texture.VulkanImage;
import net.nyalkan.vulkan.util.DrawUtil;
import org.lwjgl.opengl.GL11;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.vulkan.VK11.VK_ATTACHMENT_LOAD_OP_LOAD;
import static org.lwjgl.vulkan.VK11.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;

public class GlFramebuffer {
    private static int ID_COUNTER = 1;
    private static final Int2ReferenceOpenHashMap<GlFramebuffer> map = new Int2ReferenceOpenHashMap<>();
    private static int boundId = 0;
    private static GlFramebuffer boundFramebuffer;

    // read target for blits
    private static int readBoundId = 0;
    private static GlFramebuffer readBoundFramebuffer;

    public record Binding(int id, GlFramebuffer framebuffer, int readId, GlFramebuffer readFramebuffer) {}

    public static Binding saveBinding() {
        return new Binding(boundId, boundFramebuffer, readBoundId, readBoundFramebuffer);
    }

    public static void restoreBinding(Binding binding) {
        boundId = binding.id();
        boundFramebuffer = binding.framebuffer();
        readBoundId = binding.readId();
        readBoundFramebuffer = binding.readFramebuffer();
    }

    public static void resetBoundFramebuffer() {
        boundFramebuffer = null;
        boundId = 0;
    }

    public static void beginRendering(GlFramebuffer glFramebuffer) {
        Renderer.getInstance().beginRendering(glFramebuffer.renderPass, glFramebuffer.framebuffer);

        Framebuffer framebuffer = glFramebuffer.framebuffer;
        int viewWidth = framebuffer.getWidth();
        int viewHeight = framebuffer.getHeight();

        Renderer.setInvertedViewport(0, 0, viewWidth, viewHeight);
        Renderer.setScissor(0, 0, viewWidth, viewHeight);

        // restore after pass reset
        boundId = glFramebuffer.id;
        boundFramebuffer = glFramebuffer;
    }

    public static int genFramebufferId() {
        int id = ID_COUNTER;
        map.put(id, new GlFramebuffer(id));
        ID_COUNTER++;
        return id;
    }

    public static void bindFramebuffer(int target, int id) {
        ImmediateBatcher.INSTANCE.flushGui();
        if (isMainTarget(id))
            id = 0;

        if (target == GL30.GL_READ_FRAMEBUFFER) {
            readBoundId = id;
            readBoundFramebuffer = id == 0 ? null : map.get(id);
            return;
        }

        RenderTarget hud = id == 0 ? HudCache.captureTarget() : null;
        if (hud != null)
            id = hud.frameBufferId;

        if (boundId == id)
            return;

        if (id == 0) {
            Renderer.getInstance().endRenderPass();

            // mods swap main target
            if (Renderer.isRecording())
                Renderer.getInstance().getMainPass().rebindMainTarget();

            boundFramebuffer = null;
            boundId = 0;
        } else {
            GlFramebuffer glFramebuffer = map.get(id);

            if (glFramebuffer == null)
                throw new NullPointerException("No Framebuffer with ID: %d ".formatted(id));

            if (glFramebuffer.framebuffer != null)
                beginRendering(glFramebuffer);
            else if (glFramebuffer.colorAttachment != null)
                glFramebuffer.createAndBind();

            boundFramebuffer = glFramebuffer;
            boundId = id;
        }

        // also binds read target
        if (target == GL30.GL_FRAMEBUFFER) {
            readBoundId = boundId;
            readBoundFramebuffer = boundFramebuffer;
        }
    }

    // ignores swapped main targets
    private static boolean isMainTarget(int id) {
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        return id == 0 || main instanceof MainTarget && id == main.frameBufferId;
    }

    // whole image colour copy
    public static void blitFramebuffer(int mask) {
        if ((mask & GL11.GL_COLOR_BUFFER_BIT) == 0)
            return;

        VulkanImage src;
        if (isMainTarget(readBoundId)) {
            Renderer.getInstance().getMainPass().bindAsTexture();
            src = Vulkan.getSwapChain().getColorAttachment();
        } else {
            if (readBoundFramebuffer == null)
                return;

            src = readBoundFramebuffer.colorAttachment;
            if (src == null)
                return;
        }

        if (isMainTarget(boundId)) {
            Renderer.getInstance().getMainPass().rebindMainTarget();
        } else {
            GlFramebuffer dst = boundFramebuffer;
            if (dst == null || dst.framebuffer == null)
                return;

            beginRendering(dst);
        }

        VTextureSelector.bindTexture(0, src);
        DrawUtil.blitToScreen(!isMainTarget(readBoundId));
    }

    public static void replaceImage(VulkanImage old, @Nullable VulkanImage image) {
        for (GlFramebuffer glFramebuffer : map.values()) {
            boolean color = glFramebuffer.colorAttachment == old;
            boolean depth = glFramebuffer.depthAttachment == old;
            if (!color && !depth)
                continue;

            if (color)
                glFramebuffer.colorAttachment = image;
            if (depth)
                glFramebuffer.depthAttachment = image;

            boolean bound = glFramebuffer == boundFramebuffer;
            if (bound)
                Renderer.getInstance().endRenderPass();
            if (glFramebuffer.framebuffer != null)
                glFramebuffer.cleanUp();
            if (bound)
                glFramebuffer.createAndBind();
        }
    }

    // only bound one unbinds
    public static void deleteFramebuffer(int id) {
        GlFramebuffer glFramebuffer = id == 0 ? null : map.remove(id);
        if (glFramebuffer == null)
            return;
        if (boundFramebuffer == glFramebuffer) {
            boundFramebuffer = null;
            boundId = 0;
        }
        if (readBoundFramebuffer == glFramebuffer) {
            readBoundFramebuffer = null;
            readBoundId = 0;
        }
        if (glFramebuffer.framebuffer != null)
            glFramebuffer.cleanUp();
    }

    public static void framebufferTexture2D(int target, int attachment, int texTarget, int texture, int level) {
        // no stencil use depth
        if (attachment == GL30.GL_STENCIL_ATTACHMENT || attachment == GL30.GL_DEPTH_STENCIL_ATTACHMENT) {
            attachment = GL30.GL_DEPTH_ATTACHMENT;
        }

        if (attachment != GL30.GL_COLOR_ATTACHMENT0 && attachment != GL30.GL_DEPTH_ATTACHMENT) {
            throw new UnsupportedOperationException();
        }
        if (texTarget != GL11.GL_TEXTURE_2D) {
            throw new UnsupportedOperationException();
        }
        if (level != 0) {
            throw new UnsupportedOperationException();
        }

        if (boundFramebuffer != null)
            boundFramebuffer.setAttachmentTexture(attachment, texture);
    }

    public static void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer) {
        if (boundFramebuffer == null)
            return;

        boundFramebuffer.setAttachmentRenderbuffer(attachment, renderbuffer);
    }

    public static int glCheckFramebufferStatus(int target) {
        //TODO
        return GL30.GL_FRAMEBUFFER_COMPLETE;
    }

    // main reports own id
    public static int getBinding(boolean read) {
        int id = read ? readBoundId : boundId;
        return id == 0 ? Minecraft.getInstance().getMainRenderTarget().frameBufferId : id;
    }

    public static GlFramebuffer getBoundFramebuffer() {
        return boundFramebuffer;
    }

    public static GlFramebuffer getFramebuffer(int id) {
        return map.get(id);
    }

    private final int id;
    Framebuffer framebuffer;
    RenderPass renderPass;

    VulkanImage colorAttachment;
    VulkanImage depthAttachment;

    GlFramebuffer(int i) {
        this.id = i;
    }

    boolean beginRendering() {
        return Renderer.getInstance().beginRendering(this.renderPass, this.framebuffer);
    }

    void setAttachmentTexture(int attachment, int texture) {
        GlTexture glTexture = GlTexture.getTexture(texture);

        if (glTexture == null)
            throw new NullPointerException(String.format("Texture %d is null", texture));

        if (glTexture.vulkanImage == null)
            return;

        switch (attachment) {
            case (GL30.GL_COLOR_ATTACHMENT0) -> this.setColorAttachment(glTexture);

            case (GL30.GL_DEPTH_ATTACHMENT) -> this.setDepthAttachment(glTexture);

            default -> throw new IllegalStateException("Unexpected value: " + attachment);
        }
    }

    void setAttachmentRenderbuffer(int attachment, int texture) {
        GlRenderbuffer renderbuffer = GlRenderbuffer.getRenderbuffer(texture);

        if (renderbuffer == null)
            throw new NullPointerException(String.format("Texture %d is null", texture));

        if (renderbuffer.vulkanImage == null)
            return;

        switch (attachment) {
            case (GL30.GL_COLOR_ATTACHMENT0) -> this.setColorAttachment(renderbuffer);

            case (GL30.GL_DEPTH_ATTACHMENT) -> this.setDepthAttachment(renderbuffer);

            default -> throw new IllegalStateException("Unexpected value: " + attachment);
        }
    }

    void setColorAttachment(GlTexture texture) {
        this.colorAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setDepthAttachment(GlTexture texture) {
        //TODO check if texture is in depth format
        this.depthAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setColorAttachment(GlRenderbuffer texture) {
        this.colorAttachment = texture.vulkanImage;
        createAndBind();
    }

    void setDepthAttachment(GlRenderbuffer texture) {
        //TODO check if texture is in depth format
        this.depthAttachment = texture.vulkanImage;
        createAndBind();
    }

    void createAndBind() {
        // Cannot create without color attachment
        if (this.colorAttachment == null)
            return;

        if (this.framebuffer != null) {
            this.cleanUp();
        }

        boolean hasDepthImage = this.depthAttachment != null;
        VulkanImage depthImage = this.depthAttachment;

        this.framebuffer = Framebuffer.builder(this.colorAttachment, depthImage).build();
        RenderPass.Builder builder = RenderPass.builder(this.framebuffer);

        builder.getColorAttachmentInfo()
                .setLoadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                .setFinalLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        if (hasDepthImage)
            builder.getDepthAttachmentInfo().setOps(VK_ATTACHMENT_LOAD_OP_LOAD, VK_ATTACHMENT_LOAD_OP_LOAD);

        this.renderPass = builder.build();

        GlFramebuffer.beginRendering(this);
    }

    public Framebuffer getFramebuffer() {
        return framebuffer;
    }

    public RenderPass getRenderPass() {
        return renderPass;
    }

    void cleanUp() {
        this.framebuffer.cleanUp(false);
        this.renderPass.cleanUp();

        this.framebuffer = null;
        this.renderPass = null;
    }
}
