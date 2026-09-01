package net.nyalkan.vulkan.window;

import net.neoforged.fml.earlydisplay.DisplayWindow;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

// gl splash then vulkan window
public class VulkanImmediateWindowProvider extends DisplayWindow {

    // repaint tick is private
    private volatile boolean handoffComplete = false;

    @Override
    public void periodicTick() {
        if (this.handoffComplete) {
            GLFW.glfwPollEvents();
            return;
        }

        super.periodicTick();
    }

    @Override
    public String name() {
        return "nyalkan";
    }

    private Constructor<?> loadingOverlayConstructor;

    // addReads is caller sensitive
    @Override
    public void updateModuleReads(ModuleLayer layer) {
        var neoforgeModule = layer.findModule("neoforge").orElseThrow();
        getClass().getModule().addReads(neoforgeModule);

        var minecraftModule = layer.findModule("minecraft").orElseThrow();
        getClass().getModule().addReads(minecraftModule);
        try {
            var loadingOverlayClz = Class.forName(minecraftModule, "net.minecraft.client.gui.screens.LoadingOverlay");
            var minecraftClz = Class.forName(minecraftModule, "net.minecraft.client.Minecraft");
            var reloadInstanceClz = Class.forName(minecraftModule, "net.minecraft.server.packs.resources.ReloadInstance");
            this.loadingOverlayConstructor = loadingOverlayClz.getConstructor(minecraftClz, reloadInstanceClz, Consumer.class, boolean.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Nyalkan: failed to resolve LoadingOverlay", e);
        }
    }

    // vanilla overlay avoids gl
    @SuppressWarnings("unchecked")
    @Override
    public <T> Supplier<T> loadingOverlay(Supplier<?> mc, Supplier<?> ri, Consumer<Optional<Throwable>> ex, boolean fade) {
        return () -> {
            try {
                return (T) loadingOverlayConstructor.newInstance(mc.get(), ri.get(), ex, fade);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Nyalkan: failed to construct vanilla LoadingOverlay", e);
            }
        };
    }

    @Override
    public long setupMinecraftWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
        long glWindow = super.setupMinecraftWindow(width, height, title, monitor);

        GLFW.glfwMakeContextCurrent(0L);
        GLFW.glfwDestroyWindow(glWindow);

        // keep splash size
        int[] w = new int[1];
        int[] h = new int[1];
        int[] x = new int[1];
        int[] y = new int[1];
        this.positionWindow(Optional.empty(), v -> w[0] = v, v -> h[0] = v, v -> x[0] = v, v -> y[0] = v);

        int windowWidth = w[0] > 0 ? w[0] : width.getAsInt();
        int windowHeight = h[0] > 0 ? h[0] : height.getAsInt();

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        GLFW.glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        long vulkanWindow = GLFW.glfwCreateWindow(windowWidth, windowHeight, title.get(), 0L, 0L);
        if (vulkanWindow == 0L) {
            throw new IllegalStateException("Nyalkan: failed to create the game window");
        }

        if (x[0] != 0 || y[0] != 0) {
            GLFW.glfwSetWindowPos(vulkanWindow, x[0], y[0]);
        }

        this.handoffComplete = true;

        return vulkanWindow;
    }

    @Override
    public String getGLVersion() {
        return "N/A (Vulkan)";
    }
}
