package net.nyalkan.mixin.compat.easyvillagers;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.ref.WeakReference;

@Pseudo
@Mixin(targets = {
        "de.maxhenkel.easyvillagers.blocks.tileentity.render.VillagerRendererBase",
        "de.maxhenkel.easyvillagers.blocks.tileentity.render.IronFarmRenderer",
        "de.maxhenkel.easyvillagers.blocks.tileentity.render.ConverterRenderer",
        "de.maxhenkel.easyvillagers.blocks.tileentity.render.BreederRenderer",
        "de.maxhenkel.easyvillagers.datacomponents.VillagerData",
        "de.maxhenkel.easyvillagers.datacomponents.VillagerBlockEntityData"
}, remap = false)
public class WeakCacheM {
    @Unique private final Object[] nyalkan$strong = new Object[4];
    @Unique private int nyalkan$next;
    @Unique private int nyalkan$level;

    @WrapOperation(method = "*", at = @At(value = "NEW", target = "(Ljava/lang/Object;)Ljava/lang/ref/WeakReference;"), require = 0)
    private WeakReference<?> nyalkan$keepStrong(Object value, Operation<WeakReference<?>> original) {
        int level = System.identityHashCode(Minecraft.getInstance().level);
        if (level != this.nyalkan$level) {
            java.util.Arrays.fill(this.nyalkan$strong, null);
            this.nyalkan$level = level;
        }
        if (value != null) {
            this.nyalkan$strong[this.nyalkan$next] = value;
            this.nyalkan$next = (this.nyalkan$next + 1) & 3;
        }
        return original.call(value);
    }
}
