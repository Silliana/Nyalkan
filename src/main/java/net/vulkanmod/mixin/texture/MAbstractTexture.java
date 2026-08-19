package net.vulkanmod.mixin.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkanmod.gl.GlTexture;
import net.vulkanmod.vulkan.texture.VulkanImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractTexture.class)
public abstract class MAbstractTexture {
    @Shadow protected boolean blur;
    @Shadow protected boolean mipmap;

    @Shadow protected int id;

    @Overwrite
    public void bind() {
        if (!RenderSystem.isOnRenderThreadOrInit()) {
            RenderSystem.recordRenderCall(this::bindTexture);
        } else {
            this.bindTexture();
        }
    }

    @Inject(method = "setFilter", at = @At("HEAD"), cancellable = true)
    public void setFilter(boolean blur, boolean mipmap, CallbackInfo ci) {
        if (blur != this.blur || mipmap != this.mipmap) {
            this.blur = blur;
            this.mipmap = mipmap;

            GlTexture glTexture = GlTexture.getTexture(this.id);
            VulkanImage vulkanImage = glTexture.getVulkanImage();

            if (vulkanImage != null)
                vulkanImage.updateTextureSampler(this.blur, false, this.mipmap);
        }

        ci.cancel();
    }

    private void bindTexture() {
        GlTexture.bindTexture(this.id);
    }
}
