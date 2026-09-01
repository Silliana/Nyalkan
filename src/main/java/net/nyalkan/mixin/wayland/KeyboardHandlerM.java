package net.nyalkan.mixin.wayland;

import net.minecraft.client.KeyboardHandler;
import net.nyalkan.config.Platform;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// wayland ctrl double char
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerM {

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void cancelCtrlCharTyped(long windowPointer, int codePoint, int modifiers, CallbackInfo ci) {
        if (Platform.isWayLand() && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            ci.cancel();
        }
    }
}
