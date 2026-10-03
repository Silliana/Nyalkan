package net.nyalkan.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.nyalkan.interfaces.ExtendedSpriteContents;
import net.nyalkan.vulkan.texture.VulkanImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.util.HashSet;
import java.util.Set;

public abstract class SpriteUtil {

    private static boolean doUpload = false;

    private static Set<VulkanImage> transitionedLayouts = new HashSet<>();

    public static void setDoUpload(boolean b) {
        doUpload = b;
    }

    public static boolean shouldUpload() {
        return doUpload;
    }

    public static boolean isAnimated(TextureAtlasSprite sprite) {
        return sprite != null && ((ExtendedSpriteContents) sprite.contents()).nyalkan$isAnimated();
    }

    public static void markActive(TextureAtlasSprite sprite) {
        if (isAnimated(sprite))
            ((ExtendedSpriteContents) sprite.contents()).nyalkan$setActive(true);
    }

    public static void markActive(TextureAtlasSprite[] sprites) {
        for (TextureAtlasSprite sprite : sprites)
            ((ExtendedSpriteContents) sprite.contents()).nyalkan$setActive(true);
    }

    public static void addTransitionedLayout(VulkanImage image) {
        transitionedLayouts.add(image);
    }

    public static void transitionLayouts(VkCommandBuffer commandBuffer) {
        try(MemoryStack stack = MemoryStack.stackPush()) {
            transitionedLayouts.forEach(image -> image.readOnlyLayout(stack, commandBuffer));

            transitionedLayouts.clear();
        }
    }
}
