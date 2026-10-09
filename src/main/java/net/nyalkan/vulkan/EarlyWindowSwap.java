package net.nyalkan.vulkan;

import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import net.nyalkan.Initializer;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.function.Supplier;

import static org.lwjgl.glfw.GLFW.*;

// swaps splash for vulkan
public final class EarlyWindowSwap {
    private static final String OLD_PROVIDER = "nyalkan";
    private static final String DEFAULT_PROVIDER = "fmlearlywindow";

    private static long splashWindow;

    private EarlyWindowSwap() {}

    public static long replace(long handed, int fallbackWidth, int fallbackHeight, Supplier<String> title) {
        resetOldProvider();
        if (glfwGetWindowAttrib(handed, GLFW_CLIENT_API) == GLFW_NO_API)
            return handed;

        int[] w = new int[1], h = new int[1], x = new int[1], y = new int[1];
        glfwGetWindowSize(handed, w, h);
        glfwGetWindowPos(handed, x, y);
        glfwHideWindow(handed);
        splashWindow = handed;
        stopSplashRepaint();
        if (glfwGetCurrentContext() == handed)
            glfwMakeContextCurrent(0L);

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        long window = glfwCreateWindow(w[0] > 0 ? w[0] : fallbackWidth, h[0] > 0 ? h[0] : fallbackHeight, title.get(), 0L, 0L);
        if (window == 0L)
            throw new IllegalStateException("Nyalkan: failed to create the Vulkan game window");
        if (x[0] != 0 || y[0] != 0)
            glfwSetWindowPos(window, x[0], y[0]);

        Initializer.LOGGER.info("Nyalkan: replaced the splash window with a Vulkan window");
        return window;
    }

    // splash repaint needs gl
    private static void stopSplashRepaint() {
        try {
            Field providerField = ImmediateWindowHandler.class.getDeclaredField("provider");
            providerField.setAccessible(true);
            Object provider = providerField.get(null);
            for (Class<?> c = provider.getClass(); c != null; c = c.getSuperclass()) {
                try {
                    Field repaint = c.getDeclaredField("repaintTick");
                    repaint.setAccessible(true);
                    repaint.set(provider, (Runnable) () -> {});
                    return;
                } catch (NoSuchFieldException ignored) {
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            Initializer.LOGGER.warn("Nyalkan: could not stop the splash repaint", e);
        }
    }

    private static void resetOldProvider() {
        if (!OLD_PROVIDER.equals(FMLConfig.getConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER)))
            return;

        FMLConfig.updateConfig(FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER, DEFAULT_PROVIDER);
        Initializer.LOGGER.info("Nyalkan: reset earlyWindowProvider to '{}'", DEFAULT_PROVIDER);
    }

    public static void releaseSplash() {
        if (splashWindow == 0L)
            return;

        if (glfwGetCurrentContext() == splashWindow)
            glfwMakeContextCurrent(0L);
        glfwDestroyWindow(splashWindow);
        splashWindow = 0L;
    }
}
