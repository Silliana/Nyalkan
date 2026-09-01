package net.nyalkan.mixin.texture;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.Tickable;
import net.nyalkan.render.texture.SpriteUtil;
import net.nyalkan.vulkan.Renderer;
import net.nyalkan.vulkan.device.DeviceManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Set;

@Mixin(TextureManager.class)
public abstract class MTextureManager {

    @Shadow @Final private Set<Tickable> tickableTextures;

    @Overwrite
    public void tick() {
        if (Renderer.skipRendering)
            return;

        //Debug D
        if (SpriteUtil.shouldUpload())
            DeviceManager.getGraphicsQueue().startRecording();
        for (Tickable tickable : this.tickableTextures) {
            tickable.tick();
        }
        if (SpriteUtil.shouldUpload()) {
            SpriteUtil.transitionLayouts(DeviceManager.getGraphicsQueue().getCommandBuffer().getHandle());
            DeviceManager.getGraphicsQueue().endRecordingAndSubmit();
        }
    }
}
