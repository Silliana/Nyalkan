package net.nyalkan.mixin.compat.ae2;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.HashMap;

// reuse outline box shapes
@Pseudo
@Mixin(targets = "appeng.hooks.RenderBlockOutlineHook", remap = false)
public class OutlineBoxesM {
    @Unique private static final HashMap<AABB, VoxelShape> nyalkan$shapes = new HashMap<>();

    @WrapOperation(method = "renderBoxes", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/Shapes;create(Lnet/minecraft/world/phys/AABB;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private static VoxelShape nyalkan$sharedShape(AABB box, Operation<VoxelShape> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(box);
        VoxelShape shape = nyalkan$shapes.get(box);
        if (shape == null) {
            if (nyalkan$shapes.size() >= 512)
                nyalkan$shapes.clear();
            shape = original.call(box);
            nyalkan$shapes.put(box, shape);
        }
        return shape;
    }
}
