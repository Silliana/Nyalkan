package net.nyalkan.vulkan;

import net.nyalkan.vulkan.shader.PipelineState;

// emulated GL state backup
public final class RenderStateSnapshot {
    private boolean blendEnabled;
    private int srcRgb, dstRgb, srcAlpha, dstAlpha, blendOp;
    private boolean depthTest, depthMask, cull, logicOp;
    private int depthFun, logicOpFun, colorMask;
    private int stencilFunc, stencilRef, stencilFuncMask, stencilWriteMask;
    private int stencilFail, stencilZFail, stencilZPass;
    private float depthBiasUnits, depthBiasFactor;

    public void capture() {
        PipelineState.BlendInfo blend = PipelineState.blendInfo;
        this.blendEnabled = blend.enabled;
        this.srcRgb = blend.srcRgbFactor;
        this.dstRgb = blend.dstRgbFactor;
        this.srcAlpha = blend.srcAlphaFactor;
        this.dstAlpha = blend.dstAlphaFactor;
        this.blendOp = blend.blendOp;

        this.depthTest = VRenderSystem.depthTest;
        this.depthMask = VRenderSystem.depthMask;
        this.depthFun = VRenderSystem.depthFun;
        this.cull = VRenderSystem.cull;
        this.logicOp = VRenderSystem.logicOp;
        this.logicOpFun = VRenderSystem.logicOpFun;
        this.colorMask = VRenderSystem.colorMask;

        this.stencilFunc = VRenderSystem.stencilFunc;
        this.stencilRef = VRenderSystem.stencilRef;
        this.stencilFuncMask = VRenderSystem.stencilFuncMask;
        this.stencilWriteMask = VRenderSystem.stencilWriteMask;
        this.stencilFail = VRenderSystem.stencilFail;
        this.stencilZFail = VRenderSystem.stencilZFail;
        this.stencilZPass = VRenderSystem.stencilZPass;

        this.depthBiasUnits = Renderer.getDepthBiasUnits();
        this.depthBiasFactor = Renderer.getDepthBiasFactor();
    }

    public void restore() {
        PipelineState.BlendInfo blend = PipelineState.blendInfo;
        blend.enabled = this.blendEnabled;
        blend.srcRgbFactor = this.srcRgb;
        blend.dstRgbFactor = this.dstRgb;
        blend.srcAlphaFactor = this.srcAlpha;
        blend.dstAlphaFactor = this.dstAlpha;
        blend.blendOp = this.blendOp;

        VRenderSystem.depthTest = this.depthTest;
        VRenderSystem.depthMask = this.depthMask;
        VRenderSystem.depthFun = this.depthFun;
        VRenderSystem.cull = this.cull;
        VRenderSystem.logicOp = this.logicOp;
        VRenderSystem.logicOpFun = this.logicOpFun;
        VRenderSystem.colorMask = this.colorMask;

        VRenderSystem.stencilFunc = this.stencilFunc;
        VRenderSystem.stencilRef = this.stencilRef;
        VRenderSystem.stencilFuncMask = this.stencilFuncMask;
        VRenderSystem.stencilWriteMask = this.stencilWriteMask;
        VRenderSystem.stencilFail = this.stencilFail;
        VRenderSystem.stencilZFail = this.stencilZFail;
        VRenderSystem.stencilZPass = this.stencilZPass;

        Renderer.setDepthBias(this.depthBiasUnits, this.depthBiasFactor);
    }
}
