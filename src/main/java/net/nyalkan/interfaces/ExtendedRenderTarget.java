package net.nyalkan.interfaces;

import net.nyalkan.vulkan.framebuffer.RenderPass;

public interface ExtendedRenderTarget {

    boolean isBound();

    RenderPass getRenderPass();
}
