package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.vulkanmod.gl.GlFramebuffer;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.interfaces.ExtendedRenderTarget;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.framebuffer.Framebuffer;
import net.vulkanmod.vulkan.framebuffer.RenderPass;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import net.vulkanmod.vulkan.util.DrawUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin implements ExtendedRenderTarget {

    @Shadow public int viewWidth;
    @Shadow public int viewHeight;
    @Shadow public int width;
    @Shadow public int height;

    @Shadow protected int depthBufferId;
    @Shadow protected int colorTextureId;
    @Shadow public int frameBufferId;

    @Shadow @Final private float[] clearChannels;
    @Shadow @Final public boolean useDepth;

    boolean needClear = false;
    boolean bound = false;

    @Overwrite
    public void clear(boolean getError) {
        RenderSystem.assertOnRenderThreadOrInit();

        if(!Renderer.isRecording())
            return;

        // If the framebuffer is not bound postpone clear
        GlFramebuffer glFramebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId);
        if(!bound || GlFramebuffer.getBoundFramebuffer() != glFramebuffer) {
            needClear = true;
            return;
        }

        GlStateManager._clearColor(this.clearChannels[0], this.clearChannels[1], this.clearChannels[2], this.clearChannels[3]);
        int i = 16384;
        if (this.useDepth) {
            GlStateManager._clearDepth(1.0);
            i |= 256;
        }

        GlStateManager._clear(i, getError);
        needClear = false;
    }

    @Overwrite
    public void bindRead() {
        RenderSystem.assertOnRenderThread();

        applyClear();

        GlTexture.bindTexture(this.colorTextureId);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            GlTexture.getBoundTexture().getVulkanImage()
                    .readOnlyLayout(stack, Renderer.getCommandBuffer());
        }
    }

    @Overwrite
    public void unbindRead() {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.bindTexture(0);
    }

    @Overwrite
    private void _bindWrite(boolean bl) {
        RenderSystem.assertOnRenderThreadOrInit();

        GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);
        if (bl) {
            GlStateManager._viewport(0, 0, this.viewWidth, this.viewHeight);
        }

        this.bound = true;
        if (needClear)
            clear(false);
    }

    @Overwrite
    public void unbindWrite() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                GlStateManager._glBindFramebuffer(36160, 0);
                this.bound = false;
            });
        } else {
            GlStateManager._glBindFramebuffer(36160, 0);
            this.bound = false;
        }
    }

    @Overwrite
    public void destroyBuffers() {
    }

    @Overwrite
    public void createBuffers(int width, int height, boolean clearError) {
        this.viewWidth = width;
        this.viewHeight = height;
        this.width = width;
        this.height = height;

        this.frameBufferId = GlFramebuffer.genFramebufferId();
        this.colorTextureId = GlTexture.genTextureId();
        if (this.useDepth) {
            this.depthBufferId = GlTexture.genTextureId();
        }

        //
        if (!((Object) this instanceof MainTarget)) {
            GlFramebuffer.bindFramebuffer(GL30.GL_FRAMEBUFFER, this.frameBufferId);

            GlTexture.allocateTexture2D(this.colorTextureId, width, height, GL11.GL_RGBA8, GL11.GL_UNSIGNED_BYTE);
            GlFramebuffer.framebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, this.colorTextureId, 0);

            if (this.useDepth) {
                GlTexture.allocateTexture2D(this.depthBufferId, width, height, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT);
                GlFramebuffer.framebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, this.depthBufferId, 0);
            }
        }
    }

    @Inject(method = "_blitToScreen", at = @At("HEAD"), cancellable = true)
    private void _blitToScreen(int width, int height, boolean disableBlend, CallbackInfo ci) {
        // If the target needs clear it means it has not been used, thus we can skip blit
        if (!this.needClear) {
            Framebuffer framebuffer = GlFramebuffer.getFramebuffer(this.frameBufferId).getFramebuffer();
            VTextureSelector.bindTexture(0, framebuffer.getColorAttachment());

            DrawUtil.blitToScreen();
        }

        ci.cancel();
    }

    @Inject(method = "getColorTextureId", at = @At("HEAD"))
    private void injClear(CallbackInfoReturnable<Integer> cir) {
        applyClear();
    }

    @Override
    public boolean isBound() {
        return bound;
    }

    @Override
    public RenderPass getRenderPass() {
        return GlFramebuffer.getFramebuffer(this.frameBufferId).getRenderPass();
    }

    @Unique
    private void applyClear() {
        if (this.needClear) {
            GlFramebuffer currentFramebuffer = GlFramebuffer.getBoundFramebuffer();

            this._bindWrite(false);

            if (currentFramebuffer != null) {
                GlFramebuffer.beginRendering(currentFramebuffer);
            }
        }
    }
}
