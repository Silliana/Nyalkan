package net.nyalkan.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// biome once per section
@Mixin(ClientLevel.class)
public class AmbientBiomeM {

    @Unique private final Long2ObjectOpenHashMap<Holder<Biome>> nyalkan$ambientBiomes = new Long2ObjectOpenHashMap<>();

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void nyalkan$resetAmbientBiomes(int x, int y, int z, CallbackInfo ci) {
        this.nyalkan$ambientBiomes.clear();
    }

    @WrapOperation(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBiome(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/Holder;"))
    private Holder<Biome> nyalkan$sectionBiome(ClientLevel level, BlockPos pos, Operation<Holder<Biome>> original) {
        long key = SectionPos.asLong(pos);
        Holder<Biome> biome = this.nyalkan$ambientBiomes.get(key);
        if (biome == null) {
            biome = original.call(level, pos);
            this.nyalkan$ambientBiomes.put(key, biome);
        }
        return biome;
    }
}
