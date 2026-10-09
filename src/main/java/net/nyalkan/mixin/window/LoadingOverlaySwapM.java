package net.nyalkan.mixin.window;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.nyalkan.vulkan.EarlyWindowSwap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(Minecraft.class)
public class LoadingOverlaySwapM {
    @Shadow private Overlay overlay;

    // splash overlay uses gl
    @SuppressWarnings("unchecked")
    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;loadingOverlay(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Consumer;Z)Ljava/util/function/Supplier;", remap = false))
    private <T> Supplier<T> nyalkan$vanillaOverlay(Supplier<Minecraft> minecraft, Supplier<ReloadInstance> reload,
                                                     Consumer<Optional<Throwable>> error, boolean fade, Operation<Supplier<T>> original) {
        return () -> (T) new LoadingOverlay(minecraft.get(), reload.get(), error, fade);
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void nyalkan$releaseSplash(boolean renderLevel, CallbackInfo ci) {
        if (this.overlay == null)
            EarlyWindowSwap.releaseSplash();
    }
}
