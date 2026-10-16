package net.nyalkan.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.nyalkan.Initializer;
import net.nyalkan.config.Config;
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

    public static final int ANIM_WATER = 0;
    public static final int ANIM_LAVA = 1;
    public static final int ANIM_FIRE = 2;
    public static final int ANIM_PORTAL = 3;
    public static final int ANIM_OTHER = 4;

    public static int animationCategory(ResourceLocation name) {
        String path = name.getPath();
        if (path.contains("water"))
            return ANIM_WATER;
        if (path.contains("lava"))
            return ANIM_LAVA;
        if (path.contains("fire"))
            return ANIM_FIRE;
        if (path.contains("portal"))
            return ANIM_PORTAL;
        return ANIM_OTHER;
    }

    public static boolean animationEnabled(int category) {
        Config config = Initializer.CONFIG;
        return switch (category) {
            case ANIM_WATER -> config.animateWater;
            case ANIM_LAVA -> config.animateLava;
            case ANIM_FIRE -> config.animateFire;
            case ANIM_PORTAL -> config.animatePortal;
            default -> config.animateBlocks;
        };
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
