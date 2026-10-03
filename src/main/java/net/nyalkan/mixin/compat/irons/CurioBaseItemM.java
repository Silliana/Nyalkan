package net.nyalkan.mixin.compat.irons;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.world.entity.LivingEntity;
import net.nyalkan.vulkan.Renderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

import java.lang.ref.WeakReference;

@Pseudo
@Mixin(targets = "io.redspace.ironsspellbooks.item.curios.CurioBaseItem", remap = false)
public abstract class CurioBaseItemM {

    @Unique private WeakReference<LivingEntity> nyalkan$entity;
    @Unique private long nyalkan$frame = -1;
    @Unique private boolean nyalkan$equipped;

    @WrapMethod(method = "isEquippedBy", require = 0)
    private boolean nyalkan$cacheEquipped(LivingEntity entity, Operation<Boolean> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(entity);

        long frame = Renderer.getFrameCount();
        if (frame == this.nyalkan$frame && this.nyalkan$entity != null && this.nyalkan$entity.get() == entity)
            return this.nyalkan$equipped;

        boolean equipped = original.call(entity);
        this.nyalkan$entity = new WeakReference<>(entity);
        this.nyalkan$frame = frame;
        this.nyalkan$equipped = equipped;
        return equipped;
    }
}
