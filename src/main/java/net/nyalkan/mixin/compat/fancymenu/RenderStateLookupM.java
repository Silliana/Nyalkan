package net.nyalkan.mixin.compat.fancymenu;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = {"de.keksuccino.fancymenu.util.rendering.RenderRotationUtil", "de.keksuccino.fancymenu.util.rendering.RenderTranslationUtil"}, remap = false)
public class RenderStateLookupM {
    @Unique private static Object nyalkan$renderState;

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Ljava/lang/ThreadLocal;get()Ljava/lang/Object;"), require = 0)
    private static Object nyalkan$cached(ThreadLocal<?> local, Operation<Object> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(local);
        Object state = nyalkan$renderState;
        if (state == null)
            nyalkan$renderState = state = original.call(local);
        return state;
    }
}
