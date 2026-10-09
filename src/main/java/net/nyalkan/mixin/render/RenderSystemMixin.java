package net.nyalkan.mixin.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.nyalkan.gl.GlTexture;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.render.immediate.ImmediateBatcher;
import net.nyalkan.vulkan.VRenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.objectweb.asm.Opcodes;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.function.Consumer;

import static com.mojang.blaze3d.systems.RenderSystem.*;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Shadow private static Matrix4f projectionMatrix;
    @Shadow private static Matrix4f savedProjectionMatrix;
    @Shadow @Final private static Matrix4fStack modelViewStack;
    @Shadow private static Matrix4f modelViewMatrix;
    @Shadow private static Matrix4f textureMatrix;
    @Shadow @Final private static int[] shaderTextures;
    @Shadow @Final private static float[] shaderColor;
    @Shadow @Final private static Vector3f[] shaderLightDirections;

    @Shadow @Final private static float[] shaderFogColor;

    @Shadow private static @Nullable Thread renderThread;
    @Shadow private static double lastDrawTime;

    @Shadow
    public static void assertOnRenderThread() {
    }

    @Overwrite(remap = false)
    public static void initRenderer(int debugVerbosity, boolean debugSync) {
        VRenderSystem.initRenderer();

        renderThread.setPriority(Thread.NORM_PRIORITY + 2);
    }

    @Overwrite(remap = false)
    public static void setupDefaultState(int x, int y, int width, int height) { }

    @Overwrite(remap = false)
    public static void enableColorLogicOp() {
        assertOnRenderThread();
        VRenderSystem.enableColorLogicOp();
    }

    @Overwrite(remap = false)
    public static void disableColorLogicOp() {
        assertOnRenderThread();
        VRenderSystem.disableColorLogicOp();
    }

    @Overwrite
    public static void logicOp(GlStateManager.LogicOp op) {
        assertOnRenderThread();
        VRenderSystem.logicOp(op);
    }

    @Overwrite(remap = false)
    public static void activeTexture(int texture) {
        GlTexture.activeTexture(texture);
    }

    @Overwrite(remap = false)
    public static void glGenBuffers(Consumer<Integer> consumer) {}

    @Overwrite(remap = false)
    public static void glGenVertexArrays(Consumer<Integer> consumer) {}

    @Overwrite(remap = false)
    public static int maxSupportedTextureSize() {
        return VRenderSystem.maxSupportedTextureSize();
    }

    @Overwrite(remap = false)
    public static void clear(int mask, boolean getError) {
        VRenderSystem.clear(mask);
    }

    @Overwrite(remap = false)
    public static void clearColor(float r, float g, float b, float a) {
        VRenderSystem.setClearColor(r, g, b, a);
    }

    @Overwrite(remap = false)
    public static void clearDepth(double d) {
        VRenderSystem.clearDepth(d);
    }

    @Redirect(method = "flipFrame", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwSwapBuffers(J)V"), remap = false)
    private static void removeSwapBuffers(long window) {
    }

    @Overwrite(remap = false)
    public static void viewport(int x, int y, int width, int height) {
        Renderer.setViewport(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void enableScissor(int x, int y, int width, int height) {
        Renderer.setScissor(x, y, width, height);
    }

    @Overwrite(remap = false)
    public static void disableScissor() {
        Renderer.resetScissor();
    }

    @Overwrite(remap = false)
    public static void disableDepthTest() {
        assertOnRenderThread();
        //GlStateManager._disableDepthTest();
        VRenderSystem.disableDepthTest();
    }

    @Overwrite(remap = false)
    public static void enableDepthTest() {
        assertOnRenderThreadOrInit();
        VRenderSystem.enableDepthTest();
    }

    @Overwrite(remap = false)
    public static void depthFunc(int i) {
        assertOnRenderThread();
        VRenderSystem.depthFunc(i);
    }

    @Overwrite(remap = false)
    public static void depthMask(boolean b) {
        assertOnRenderThread();
        VRenderSystem.depthMask(b);
    }

    @Overwrite(remap = false)
    public static void colorMask(boolean red, boolean green, boolean blue, boolean alpha) {
        VRenderSystem.colorMask(red, green, blue, alpha);
    }

    @Overwrite(remap = false)
    public static void blendEquation(int i) {
        assertOnRenderThread();
        //TODO
    }

    @Overwrite(remap = false)
    public static void enableBlend() {
        VRenderSystem.enableBlend();
    }

    @Overwrite(remap = false)
    public static void disableBlend() {
        VRenderSystem.disableBlend();
    }

    @Overwrite(remap = false)
    public static void blendFunc(GlStateManager.SourceFactor sourceFactor, GlStateManager.DestFactor destFactor) {
        VRenderSystem.blendFunc(sourceFactor, destFactor);
    }

    @Overwrite(remap = false)
    public static void blendFunc(int srcFactor, int dstFactor) {
        VRenderSystem.blendFunc(srcFactor, dstFactor);
    }

    @Overwrite(remap = false)
    public static void blendFuncSeparate(GlStateManager.SourceFactor p_69417_, GlStateManager.DestFactor p_69418_, GlStateManager.SourceFactor p_69419_, GlStateManager.DestFactor p_69420_) {
        VRenderSystem.blendFuncSeparate(p_69417_, p_69418_, p_69419_, p_69420_);
    }

    @Overwrite(remap = false)
    public static void blendFuncSeparate(int srcFactorRGB, int dstFactorRGB, int srcFactorAlpha, int dstFactorAlpha) {
        VRenderSystem.blendFuncSeparate(srcFactorRGB, dstFactorRGB, srcFactorAlpha, dstFactorAlpha);
    }

    @Overwrite(remap = false)
    public static void enableCull() {
        assertOnRenderThread();
        VRenderSystem.enableCull();
    }

    @Overwrite(remap = false)
    public static void disableCull() {
        assertOnRenderThread();
        VRenderSystem.disableCull();
    }

    @Overwrite(remap = false)
    public static void polygonMode(final int face, final int mode) {
        assertOnRenderThread();
        VRenderSystem.setPolygonModeGL(mode);
    }

    @Overwrite(remap = false)
    public static void enablePolygonOffset() {
        assertOnRenderThread();
        VRenderSystem.enablePolygonOffset();
    }

    @Overwrite(remap = false)
    public static void disablePolygonOffset() {
        assertOnRenderThread();
        VRenderSystem.disablePolygonOffset();
    }

    @Overwrite(remap = false)
    public static void polygonOffset(float p_69864_, float p_69865_) {
        assertOnRenderThread();
        VRenderSystem.polygonOffset(p_69864_, p_69865_);
    }

    @Overwrite(remap = false)
    public static void setShaderLights(Vector3f dir0, Vector3f dir1) {
        if (VRenderSystem.lightDirection0.buffer.getFloat(0) != dir0.x() || VRenderSystem.lightDirection0.buffer.getFloat(4) != dir0.y()
                || VRenderSystem.lightDirection0.buffer.getFloat(8) != dir0.z() || VRenderSystem.lightDirection1.buffer.getFloat(0) != dir1.x()
                || VRenderSystem.lightDirection1.buffer.getFloat(4) != dir1.y() || VRenderSystem.lightDirection1.buffer.getFloat(8) != dir1.z())
            ImmediateBatcher.INSTANCE.beforeStateChange();

        shaderLightDirections[0] = dir0;
        shaderLightDirections[1] = dir1;

        VRenderSystem.lightDirection0.buffer.putFloat(0, dir0.x());
        VRenderSystem.lightDirection0.buffer.putFloat(4, dir0.y());
        VRenderSystem.lightDirection0.buffer.putFloat(8, dir0.z());

        VRenderSystem.lightDirection1.buffer.putFloat(0, dir1.x());
        VRenderSystem.lightDirection1.buffer.putFloat(4, dir1.y());
        VRenderSystem.lightDirection1.buffer.putFloat(8, dir1.z());
    }

    @Overwrite(remap = false)
    private static void _setShaderColor(float r, float g, float b, float a) {
        if (shaderColor[0] != r || shaderColor[1] != g || shaderColor[2] != b || shaderColor[3] != a)
            ImmediateBatcher.INSTANCE.beforeStateChange();

        shaderColor[0] = r;
        shaderColor[1] = g;
        shaderColor[2] = b;
        shaderColor[3] = a;

        VRenderSystem.setShaderColor(r, g, b, a);
    }

    @Overwrite(remap = false)
    public static void setShaderFogColor(float f, float g, float h, float i) {
        if (shaderFogColor[0] != f || shaderFogColor[1] != g || shaderFogColor[2] != h || shaderFogColor[3] != i)
            ImmediateBatcher.INSTANCE.beforeStateChange();

        shaderFogColor[0] = f;
        shaderFogColor[1] = g;
        shaderFogColor[2] = h;
        shaderFogColor[3] = i;

        VRenderSystem.setShaderFogColor(f, g, h, i);
    }

    @Overwrite(remap = false)
    public static void setProjectionMatrix(Matrix4f projectionMatrix, VertexSorting vertexSorting) {
        Matrix4f matrix4f = new Matrix4f(projectionMatrix);
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                ImmediateBatcher.INSTANCE.beforeStateChange();
                RenderSystemMixin.projectionMatrix = matrix4f;

                VRenderSystem.applyProjectionMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            if (!RenderSystemMixin.projectionMatrix.equals(matrix4f))
                ImmediateBatcher.INSTANCE.beforeStateChange();
            RenderSystemMixin.projectionMatrix = matrix4f;

            VRenderSystem.applyProjectionMatrix(matrix4f);
            VRenderSystem.calculateMVP();
        }

    }

    @Overwrite(remap = false)
    public static void setTextureMatrix(Matrix4f matrix4f) {
        Matrix4f matrix4f2 = new Matrix4f(matrix4f);
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                ImmediateBatcher.INSTANCE.beforeStateChange();
                textureMatrix = matrix4f2;
                VRenderSystem.setTextureMatrix(matrix4f);
            });
        } else {
            if (!textureMatrix.equals(matrix4f2))
                ImmediateBatcher.INSTANCE.beforeStateChange();
            textureMatrix = matrix4f2;
            VRenderSystem.setTextureMatrix(matrix4f);
        }
    }

    @Overwrite(remap = false)
    public static void resetTextureMatrix() {
        if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
                ImmediateBatcher.INSTANCE.beforeStateChange();
                textureMatrix.identity();
            });
        } else {
            if ((textureMatrix.properties() & Matrix4f.PROPERTY_IDENTITY) == 0)
                ImmediateBatcher.INSTANCE.beforeStateChange();
            textureMatrix.identity();
            VRenderSystem.setTextureMatrix(textureMatrix);
        }
    }

    @Overwrite(remap = false)
    public static void applyModelViewMatrix() {
        Matrix4f matrix4f = new Matrix4f(modelViewStack);
        if (!isOnRenderThread()) {
            recordRenderCall(() -> {
                ImmediateBatcher.INSTANCE.beforeStateChange();
                modelViewMatrix = matrix4f;
                //Vulkan
                VRenderSystem.applyModelViewMatrix(matrix4f);
                VRenderSystem.calculateMVP();
            });
        } else {
            if (!modelViewMatrix.equals(matrix4f))
                ImmediateBatcher.INSTANCE.beforeStateChange();
            modelViewMatrix = matrix4f;

            VRenderSystem.applyModelViewMatrix(matrix4f);
            VRenderSystem.calculateMVP();
        }

    }

    @Overwrite(remap = false)
    private static void _restoreProjectionMatrix() {
        if (!projectionMatrix.equals(savedProjectionMatrix))
            ImmediateBatcher.INSTANCE.beforeStateChange();
        projectionMatrix = savedProjectionMatrix;

        VRenderSystem.applyProjectionMatrix(projectionMatrix);
        VRenderSystem.calculateMVP();
    }

    @Overwrite(remap = false)
    public static void texParameter(int target, int pname, int param) {
        GlTexture.texParameteri(target, pname, param);
    }

    @WrapOperation(method = "limitDisplayFPS", at = @At(value = "FIELD", target = "Lcom/mojang/blaze3d/systems/RenderSystem;lastDrawTime:D", opcode = Opcodes.PUTSTATIC))
    private static void nyalkan$steadyCap(double now, Operation<Void> original, @Local(ordinal = 0) double target) {
        double interval = target - lastDrawTime;
        original.call(now - target < interval ? target : now);
    }
}
