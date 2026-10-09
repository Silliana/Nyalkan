package net.nyalkan.mixin.render.sky;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.CubicSampler;
import net.minecraft.world.phys.Vec3;
import net.nyalkan.render.SkyColorCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientLevel.class)
public class ClientLevelSkyM {
    @WrapOperation(method = "getSkyColor", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 nyalkan$reuseSky(Vec3 at, CubicSampler.Vec3Fetcher fetcher, Operation<Vec3> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(at, fetcher);
        return SkyColorCache.sample(this, at, () -> original.call(at, fetcher));
    }
}
