package net.nyalkan.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkInstance;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memPointerBufferSafe;

// vulkan off boot layer
public final class GlfwVulkan {
    private static final long GET_REQUIRED_INSTANCE_EXTENSIONS = function("glfwGetRequiredInstanceExtensions");
    private static final long CREATE_WINDOW_SURFACE = function("glfwCreateWindowSurface");

    private GlfwVulkan() {}

    private static long function(String name) {
        long address = GLFW.getLibrary().getFunctionAddress(name);
        if (address == 0L)
            throw new IllegalStateException("Nyalkan: GLFW has no " + name);
        return address;
    }

    public static PointerBuffer getRequiredInstanceExtensions() {
        MemoryStack stack = MemoryStack.stackGet();
        int pointer = stack.getPointer();
        try {
            IntBuffer count = stack.callocInt(1);
            long names = JNI.invokePP(memAddress(count), GET_REQUIRED_INSTANCE_EXTENSIONS);
            return memPointerBufferSafe(names, count.get(0));
        } finally {
            stack.setPointer(pointer);
        }
    }

    public static int createWindowSurface(VkInstance instance, long window, LongBuffer surface) {
        return JNI.invokePPPPI(instance.address(), window, 0L, memAddress(surface), CREATE_WINDOW_SURFACE);
    }
}
