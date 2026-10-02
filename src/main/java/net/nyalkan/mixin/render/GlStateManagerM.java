package net.nyalkan.mixin.render;

import java.util.WeakHashMap;
import java.util.Map;
import org.spongepowered.asm.mixin.Unique;
import net.nyalkan.vulkan.RenderStateSnapshot;
import net.neoforged.neoforge.client.GlStateBackup;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.nyalkan.gl.GlBuffer;
import net.nyalkan.gl.GlFramebuffer;
import net.nyalkan.gl.GlRenderbuffer;
import net.nyalkan.gl.GlTexture;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.function.Consumer;

@Mixin(GlStateManager.class)
public class GlStateManagerM {

    @Overwrite(remap = false)
    public static void _bindTexture(int i) {
        GlTexture.bindTexture(i);
    }

    @Overwrite(remap = false)
    public static void _disableBlend() {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.disableBlend();
    }

    @Overwrite(remap = false)
    public static void _enableBlend() {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.enableBlend();
    }

    @Overwrite(remap = false)
    public static void _blendFunc(int i, int j) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.blendFunc(i, j);

    }

    @Overwrite(remap = false)
    public static void _blendFuncSeparate(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.blendFuncSeparate(i, j, k, l);

    }

    @Overwrite(remap = false)
    public static void _disableScissorTest() {
        ImmediateBatcher.INSTANCE.flushGui();
        VRenderSystem.scissorTest = false;
        Renderer.resetScissor();
    }

    @Overwrite(remap = false)
    public static void _enableScissorTest() {
        ImmediateBatcher.INSTANCE.flushGui();
        VRenderSystem.scissorTest = true;
    }

    @Overwrite(remap = false)
    public static void _enableCull() {
        VRenderSystem.enableCull();
    }

    @Overwrite(remap = false)
    public static void _disableCull() {
        VRenderSystem.disableCull();
    }

    @Overwrite(remap = false)
    public static void _viewport(int x, int y, int width, int height) {
        ImmediateBatcher.INSTANCE.flushGui();
        Renderer.setViewport(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void _scissorBox(int x, int y, int width, int height) {
        ImmediateBatcher.INSTANCE.flushGui();
        int[] box = VRenderSystem.scissorBox;
        box[0] = x;
        box[1] = y;
        box[2] = width;
        box[3] = height;
        Renderer.setScissor(x, y, width, height);
    }

    //TODO
    @Overwrite(remap = false)
    public static int _getError() {
        return 0;
    }

    @Overwrite(remap = false)
    public static void _texImage2D(int target, int level, int internalFormat, int width, int height, int border, int format, int type, @Nullable IntBuffer pixels) {
        GlTexture.texImage2D(target, level, internalFormat, width, height, border, format, type, pixels != null ? MemoryUtil.memByteBuffer(pixels) : null);
    }

    // font glyph upload path
    @Overwrite(remap = false)
    public static void _texSubImage2D(int target, int level, int offsetX, int offsetY, int width, int height, int format, int type, long pixels) {
        GlTexture.texSubImage2D(target, level, offsetX, offsetY, width, height, format, type, pixels);
    }

    @Overwrite(remap = false)
    public static void _activeTexture(int i) {
        GlTexture.activeTexture(i);
    }

    @Overwrite(remap = false)
    public static void _texParameter(int i, int j, int k) {
        GlTexture.texParameteri(i, j, k);
    }

    @Overwrite(remap = false)
    public static void _texParameter(int i, int j, float k) {
        //TODO
    }

    @Overwrite(remap = false)
    public static int _getInteger(int i) {
        if (i == GL11.GL_MAX_TEXTURE_SIZE)
            return VRenderSystem.maxSupportedTextureSize();
        if (i == GL30.GL_DRAW_FRAMEBUFFER_BINDING || i == GL30.GL_READ_FRAMEBUFFER_BINDING)
            return GlFramebuffer.getBinding(i == GL30.GL_READ_FRAMEBUFFER_BINDING);
        return 0;
    }

    @Overwrite(remap = false)
    public static int _getTexLevelParameter(int i, int j, int k) {
        return GlTexture.getTexLevelParameter(i, j, k);
    }

    @Overwrite(remap = false)
    public static void _pixelStore(int pname, int param) {
        //Used during upload to set copy offsets
    }

    @Overwrite(remap = false)
    public static int _genTexture() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlTexture.genTextureId();
    }

    @Overwrite(remap = false)
    public static void _deleteTexture(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlTexture.glDeleteTextures(i);
    }

    @Overwrite(remap = false)
    public static void upload(int level, int xOffset, int yOffset, int width, int height, NativeImage.Format format, IntBuffer pixels, Consumer<IntBuffer> onUpload) {
        RenderSystem.assertOnRenderThreadOrInit();

        ByteBuffer byteBuffer = MemoryUtil.memByteBuffer(MemoryUtil.memAddress(pixels), pixels.remaining() * Integer.BYTES);
        GlTexture.texSubImage2D(GL11.GL_TEXTURE_2D, level, xOffset, yOffset, width, height, format.glFormat(), GL11.GL_UNSIGNED_BYTE, byteBuffer);

        onUpload.accept(pixels);
    }

    @Overwrite(remap = false)
    public static void _colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    @Overwrite(remap = false)
    public static void _depthFunc(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.depthFunc(i);
    }

    @Overwrite(remap = false)
    public static void _clearColor(float f, float g, float h, float i) {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.setClearColor(f, g, h, i);
    }

    @Overwrite(remap = false)
    public static void _clearDepth(double d) {}

    @Overwrite(remap = false)
    public static void _clear(int mask, boolean bl) {
        ImmediateBatcher.INSTANCE.flushGui();
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.clear(mask);
    }

    @Overwrite(remap = false)
    public static void _glUseProgram(int i) {}

    @Overwrite(remap = false)
    public static void _disableDepthTest() {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.disableDepthTest();
    }

    @Overwrite(remap = false)
    public static void _enableDepthTest() {
        RenderSystem.assertOnRenderThreadOrInit();
        VRenderSystem.enableDepthTest();
    }

    @Overwrite(remap = false)
    public static void _depthMask(boolean bl) {
        RenderSystem.assertOnRenderThread();
        VRenderSystem.depthMask(bl);

    }

    @Inject(method = "glGenFramebuffers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nyalkan$glGenFramebuffers(CallbackInfoReturnable<Integer> cir) {
        RenderSystem.assertOnRenderThreadOrInit();
        cir.setReturnValue(GlFramebuffer.genFramebufferId());
    }

    @Inject(method = "glGenRenderbuffers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nyalkan$glGenRenderbuffers(CallbackInfoReturnable<Integer> cir) {
        RenderSystem.assertOnRenderThreadOrInit();
        cir.setReturnValue(GlRenderbuffer.genId());
    }

    @Overwrite(remap = false)
    public static void _glBindFramebuffer(int i, int j) {
        ImmediateBatcher.INSTANCE.flushGui();
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.bindFramebuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glBlitFrameBuffer(int srcX0, int srcY0, int srcX1, int srcY1, int dstX0, int dstY0, int dstX1, int dstY1, int mask, int filter) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.blitFramebuffer(mask);
    }

    @Overwrite(remap = false)
    public static void _glFramebufferTexture2D(int i, int j, int k, int l, int m) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.framebufferTexture2D(i, j, k, l, m);
    }

    @Overwrite(remap = false)
    public static void _glBindRenderbuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlRenderbuffer.bindRenderbuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glFramebufferRenderbuffer(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlFramebuffer.framebufferRenderbuffer(i, j, k, l);
    }

    @Overwrite(remap = false)
    public static void _glRenderbufferStorage(int i, int j, int k, int l) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlRenderbuffer.renderbufferStorage(i, j, k, l);
    }

    @Overwrite(remap = false)
    public static int glCheckFramebufferStatus(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlFramebuffer.glCheckFramebufferStatus(i);
    }

    @Inject(method = "_glGenBuffers", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nyalkan$_glGenBuffers(CallbackInfoReturnable<Integer> cir) {
        RenderSystem.assertOnRenderThreadOrInit();
        cir.setReturnValue(GlBuffer.glGenBuffers());
    }

    // unhandled vao noop
    @Inject(method = "_glGenVertexArrays", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nyalkan$_glGenVertexArrays(CallbackInfoReturnable<Integer> cir) {
        RenderSystem.assertOnRenderThreadOrInit();
        cir.setReturnValue(GlTexture.genTextureId());
    }

    @Overwrite(remap = false)
    public static void _glBindVertexArray(int i) {
    }

    @Overwrite(remap = false)
    public static void _glDeleteVertexArrays(int i) {
    }

    @Overwrite(remap = false)
    public static void _glBindBuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBindBuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glBufferData(int i, ByteBuffer byteBuffer, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBufferData(i, byteBuffer, j);
    }

    @Overwrite(remap = false)
    public static void _glBufferData(int i, long l, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glBufferData(i, l, j);
    }

    @Overwrite(remap = false)
    @Nullable
    public static ByteBuffer _glMapBuffer(int i, int j) {
        RenderSystem.assertOnRenderThreadOrInit();
        return GlBuffer.glMapBuffer(i, j);
    }

    @Overwrite(remap = false)
    public static void _glUnmapBuffer(int i) {
        RenderSystem.assertOnRenderThreadOrInit();
        GlBuffer.glUnmapBuffer(i);
    }

    @Overwrite(remap = false)
    public static void _glDeleteBuffers(int i) {
        RenderSystem.assertOnRenderThread();
        GlBuffer.glDeleteBuffers(i);
    }

    @Overwrite(remap = false)
    public static void _disableVertexAttribArray(int i) {}

    @Overwrite(remap = false)
    public static void _stencilFunc(int func, int ref, int mask) {
        ImmediateBatcher.INSTANCE.flushGui();
        VRenderSystem.stencilFunc(func, ref, mask);
    }

    @Overwrite(remap = false)
    public static void _stencilMask(int mask) {
        ImmediateBatcher.INSTANCE.flushGui();
        VRenderSystem.stencilMask(mask);
    }

    @Overwrite(remap = false)
    public static void _stencilOp(int sfail, int dpfail, int dppass) {
        ImmediateBatcher.INSTANCE.flushGui();
        VRenderSystem.stencilOp(sfail, dpfail, dppass);
    }

    @Overwrite(remap = false)
    public static void _polygonOffset(float factor, float units) {
        Renderer.setDepthBias(units, factor);
    }

    @Overwrite(remap = false)
    public static void _logicOp(int logicOperation) {}

    // vanilla cache is stale
    @Unique
    private static final Map<GlStateBackup, RenderStateSnapshot> nyalkan$backups = new WeakHashMap<>();

    @Overwrite(remap = false)
    public static void _backupGlState(GlStateBackup state) {
        nyalkan$backups.computeIfAbsent(state, k -> new RenderStateSnapshot()).capture();
    }

    @Overwrite(remap = false)
    public static void _restoreGlState(GlStateBackup state) {
        RenderStateSnapshot snapshot = nyalkan$backups.get(state);
        if (snapshot != null)
            snapshot.restore();
    }
}
