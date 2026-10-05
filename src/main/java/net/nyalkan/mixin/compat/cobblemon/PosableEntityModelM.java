package net.nyalkan.mixin.compat.cobblemon;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.nyalkan.render.model.AnimationLod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Pseudo
@Mixin(targets = "com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel", remap = false)
public abstract class PosableEntityModelM {
    @Unique private static MethodHandle nyalkan$getPosable;
    @Unique private static MethodHandle nyalkan$getRoot;
    @Unique private static boolean nyalkan$failed;

    @WrapMethod(method = "setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", require = 0)
    private void nyalkan$lodAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, Operation<Void> original) {
        Object posable = RenderSystem.isOnRenderThread() ? nyalkan$posable(this) : null;
        ModelPart root = posable != null ? nyalkan$root(posable) : null;
        if (root == null) {
            original.call(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
            return;
        }
        AnimationLod.animate(entity, posable, root,
                () -> original.call(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch));
    }

    @Unique
    private static Object nyalkan$posable(Object model) {
        if (nyalkan$failed)
            return null;
        try {
            if (nyalkan$getPosable == null)
                nyalkan$getPosable = MethodHandles.publicLookup().findVirtual(
                        nyalkan$type(model, "com.cobblemon.mod.common.client.render.models.blockbench.PosableEntityModel"), "getPosableModel",
                        MethodType.methodType(nyalkan$type(model, "com.cobblemon.mod.common.client.render.models.blockbench.PosableModel")));
            return nyalkan$getPosable.invoke(model);
        } catch (Throwable t) {
            nyalkan$failed = true;
            return null;
        }
    }

    @Unique
    private static Class<?> nyalkan$type(Object owner, String name) throws ClassNotFoundException {
        return Class.forName(name, false, owner.getClass().getClassLoader());
    }

    @Unique
    private static ModelPart nyalkan$root(Object posable) {
        if (nyalkan$failed)
            return null;
        try {
            if (nyalkan$getRoot == null)
                nyalkan$getRoot = MethodHandles.publicLookup().findVirtual(
                        nyalkan$type(posable, "com.cobblemon.mod.common.client.render.models.blockbench.PosableModel"), "getRootPart",
                        MethodType.methodType(nyalkan$type(posable, "com.cobblemon.mod.common.client.render.models.blockbench.pose.Bone")));
            Object root = nyalkan$getRoot.invoke(posable);
            return root instanceof ModelPart part ? part : null;
        } catch (Throwable t) {
            nyalkan$failed = true;
            return null;
        }
    }
}
