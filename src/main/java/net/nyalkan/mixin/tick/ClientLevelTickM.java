package net.nyalkan.mixin.tick;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTickList;
import net.nyalkan.tick.TickSpreader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ClientLevel.class)
public class ClientLevelTickM {

    @WrapOperation(method = "tickEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
    private void nyalkan$spreadTicks(EntityTickList list, Consumer<Entity> action, Operation<Void> original) {
        original.call(list, TickSpreader.beginTick((ClientLevel) (Object) this, action));
    }

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void nyalkan$dropQueued(CallbackInfo ci) {
        TickSpreader.clear();
    }
}
