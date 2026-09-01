package net.nyalkan.mixin.compat.veil;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// raw vertex pointer noop
public class LegacyVertexAttributeM {

    @Mixin(targets = "foundry.veil.impl.client.render.vertex.LegacyVertexAttribBindingBuilder$FloatAttribute", remap = false)
    public static class FloatAttributeM {
        @Redirect(method = "apply", remap = false, require = 0,
                at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glVertexAttribPointer(IIIZIJ)V", remap = false))
        private void nyalkan$glVertexAttribPointer(int index, int size, int type, boolean normalized, int stride, long pointer) {
        }

        @Redirect(method = "apply", remap = false, require = 0,
                at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glVertexAttribDivisor(II)V", remap = false))
        private void nyalkan$glVertexAttribDivisor(int index, int divisor) {
        }
    }

    @Mixin(targets = "foundry.veil.impl.client.render.vertex.LegacyVertexAttribBindingBuilder$IntAttribute", remap = false)
    public static class IntAttributeM {
        @Redirect(method = "apply", remap = false, require = 0,
                at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glVertexAttribIPointer(IIIIJ)V", remap = false))
        private void nyalkan$glVertexAttribIPointer(int index, int size, int type, int stride, long pointer) {
        }

        @Redirect(method = "apply", remap = false, require = 0,
                at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glVertexAttribDivisor(II)V", remap = false))
        private void nyalkan$glVertexAttribDivisor(int index, int divisor) {
        }
    }

    @Mixin(targets = "foundry.veil.impl.client.render.vertex.LegacyVertexAttribBindingBuilder$LongAttribute", remap = false)
    public static class LongAttributeM {
        @Redirect(method = "apply", remap = false, require = 0,
                at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/ARBVertexAttrib64Bit;glVertexAttribLPointer(IIIIJ)V", remap = false))
        private void nyalkan$glVertexAttribLPointer(int index, int size, int type, int stride, long pointer) {
        }

        @Redirect(method = "apply", remap = false, require = 0,
                at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL33C;glVertexAttribDivisor(II)V", remap = false))
        private void nyalkan$glVertexAttribDivisor(int index, int divisor) {
        }
    }
}
