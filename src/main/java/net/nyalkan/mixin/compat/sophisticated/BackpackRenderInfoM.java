package net.nyalkan.mixin.compat.sophisticated;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.BackpackRenderInfo", remap = false)
public abstract class BackpackRenderInfoM {
    @Unique private static final Map<ItemStack, Object[]> nyalkan$cache = new WeakHashMap<>();
    @Unique private static DataComponentType<?> nyalkan$type;
    @Unique private static boolean nyalkan$failed;

    @Inject(method = "fromPhysicalStack", at = @At("HEAD"), cancellable = true, require = 0)
    private static void nyalkan$cachedInfo(ItemStack stack, CallbackInfoReturnable<Object> cir) {
        DataComponentType<?> type = nyalkan$type();
        if (type == null || !RenderSystem.isOnRenderThread())
            return;
        // components replaced on change
        Object[] cached = nyalkan$cache.get(stack);
        if (cached != null && cached[0] == stack.get(type))
            cir.setReturnValue(cached[1]);
    }

    @Inject(method = "fromPhysicalStack", at = @At("RETURN"), require = 0)
    private static void nyalkan$storeInfo(ItemStack stack, CallbackInfoReturnable<Object> cir) {
        DataComponentType<?> type = nyalkan$type();
        if (type != null && RenderSystem.isOnRenderThread())
            nyalkan$cache.put(stack, new Object[]{stack.get(type), cir.getReturnValue()});
    }

    @Unique
    private static DataComponentType<?> nyalkan$type() {
        if (nyalkan$type == null && !nyalkan$failed) {
            try {
                Supplier<?> supplier = (Supplier<?>) Class.forName("net.p3pp3rf1y.sophisticatedcore.init.ModCoreDataComponents")
                        .getField("RENDER_INFO_TAG").get(null);
                nyalkan$type = (DataComponentType<?>) supplier.get();
            } catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
                nyalkan$failed = true;
            }
        }
        return nyalkan$type;
    }
}
