package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw vertex attrib noop
@Mixin(targets = "foundry.veil.impl.client.render.vertex.LegacyVertexAttribBindingBuilder", remap = false)
public class LegacyVertexAttribBindingBuilderM {

    @Redirect(method = {"setVertexAttribute", "setVertexIAttribute", "setVertexLAttribute"},
            remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glEnableVertexAttribArray(I)V", remap = false))
    private void nyalkan$glEnableVertexAttribArray(int index) {
    }

    @Redirect(method = {"removeAttribute", "clearVertexAttributes"}, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glDisableVertexAttribArray(I)V", remap = false))
    private void nyalkan$glDisableVertexAttribArray(int index) {
    }
}
