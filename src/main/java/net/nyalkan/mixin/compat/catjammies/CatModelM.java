package net.nyalkan.mixin.compat.catjammies;

import net.minecraft.client.model.CatModel;
import net.minecraft.world.entity.animal.Cat;
import net.nyalkan.render.compat.CatJammiesCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CatModel.class)
public class CatModelM {

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/animal/Cat;FFFFF)V", at = @At("HEAD"), require = 0)
    private void nyalkan$wrapDetectors(Cat cat, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        CatJammiesCache.ensureWrapped();
    }
}
