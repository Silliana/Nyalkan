package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// dummy vao id
@Mixin(targets = "foundry.veil.api.client.render.VeilRenderSystem", remap = false)
public class VeilRenderSystemVaoM {

    @Redirect(method = "init", remap = false,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL43C;glGenVertexArrays()I", remap = false))
    private static int nyalkan$glGenVertexArrays() {
        return 0;
    }

    @Redirect(method = "init", remap = false,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBDirectStateAccess;glCreateVertexArrays()I", remap = false))
    private static int nyalkan$glCreateVertexArrays() {
        return 0;
    }
}
