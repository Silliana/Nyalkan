package net.nyalkan.mixin.window;

import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.nyalkan.Initializer;
import net.nyalkan.config.Config;
import net.nyalkan.config.video.VideoModeManager;
import net.nyalkan.config.option.Options;
import net.nyalkan.config.video.VideoModeSet;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.VRenderSystem;
import net.nyalkan.vulkan.Vulkan;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.glfw.GLFW.*;

@Mixin(Window.class)
public abstract class WindowMixin {
    @Final @Shadow private long window;

    @Shadow private boolean vsync;

    @Shadow protected abstract void updateFullscreen(boolean bl);

    @Shadow private boolean fullscreen;

    @Shadow @Final private static Logger LOGGER;

    @Shadow private int windowedX;
    @Shadow private int windowedY;
    @Shadow private int windowedWidth;
    @Shadow private int windowedHeight;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int width;
    @Shadow private int height;

    @Shadow private int framebufferWidth;
    @Shadow private int framebufferHeight;

    @Unique private static final int MAX_TEXTURE_SIZE = 16384;
    @Unique private boolean nyalkan$wasOnFullscreen;

    // no gl context exists

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwMakeContextCurrent(J)V"))
    private void redirectMakeContextCurrent(long window) { }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL;createCapabilities()Lorg/lwjgl/opengl/GLCapabilities;"))
    private GLCapabilities redirectCreateCapabilities() {
        return null;
    }

    // safe until device known
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;maxSupportedTextureSize()I"))
    private int redirectMaxSupportedTextureSize() {
        return MAX_TEXTURE_SIZE;
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void getHandle(WindowEventHandler windowEventHandler, ScreenManager screenManager, DisplayData displayData, String string, String string2, CallbackInfo ci) {
        VRenderSystem.setWindow(this.window);
    }

    @Overwrite
    public void updateVsync(boolean vsync) {
        this.vsync = vsync;
        Vulkan.setVsync(vsync);
    }

    @Overwrite
    public void toggleFullScreen() {
        this.fullscreen = !this.fullscreen;
        Options.fullscreenDirty = true;
    }

    @Overwrite
    public void updateDisplay() {
        RenderSystem.flipFrame(this.window);

        if (Options.fullscreenDirty) {
            Options.fullscreenDirty = false;
            this.updateFullscreen(this.vsync);
        }
    }

    @Overwrite
    private void setMode() {
        Config config = Initializer.CONFIG;

        // first call precedes config
        if (config == null) {
            GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
            return;
        }

        if (this.fullscreen) {
            VideoModeSet.VideoMode videoMode = config.videoMode;
            VideoModeSet set = VideoModeManager.getFromVideoMode(videoMode);
            if (set == null || !set.hasRefreshRate(videoMode.refreshRate)) {
                LOGGER.error("Resolution not supported, using first available as fallback");
                videoMode = VideoModeManager.getFirstAvailable().getVideoMode();
            }

            this.nyalkan$saveWindowed();
            this.x = 0;
            this.y = 0;
            this.width = videoMode.width;
            this.height = videoMode.height;
            GLFW.glfwSetWindowMonitor(this.window, GLFW.glfwGetPrimaryMonitor(), this.x, this.y, this.width, this.height, videoMode.refreshRate);
            this.nyalkan$wasOnFullscreen = true;
        } else if (config.windowedFullscreen) {
            VideoModeSet.VideoMode videoMode = VideoModeManager.getOsVideoMode();

            this.nyalkan$saveWindowed();
            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_FALSE);
            GLFW.glfwSetWindowMonitor(this.window, 0L, 0, 0, videoMode.width, videoMode.height, -1);
            this.width = videoMode.width;
            this.height = videoMode.height;
            this.nyalkan$wasOnFullscreen = true;
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;
            GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
            GLFW.glfwSetWindowAttrib(this.window, GLFW_DECORATED, GLFW_TRUE);
            this.nyalkan$wasOnFullscreen = false;
        }
    }

    @Unique
    private void nyalkan$saveWindowed() {
        if (this.nyalkan$wasOnFullscreen)
            return;

        this.windowedX = this.x;
        this.windowedY = this.y;
        this.windowedWidth = this.width;
        this.windowedHeight = this.height;
    }

    @Overwrite
    private void onFramebufferResize(long window, int width, int height) {
        if (window == this.window && width > 0 && height > 0) {
            this.framebufferWidth = width;
            this.framebufferHeight = height;
            Renderer.scheduleSwapChainUpdate();
        }
    }

    @Overwrite
    private void onResize(long window, int width, int height) {
        this.width = width;
        this.height = height;

        if(width > 0 && height > 0)
            Renderer.scheduleSwapChainUpdate();
    }

}
