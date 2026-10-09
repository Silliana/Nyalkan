package net.nyalkan.mixin.render.block;

import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ChestRenderer.class)
public interface ChestRendererAccessor {
    @Invoker("getMaterial")
    Material nyalkan$getMaterial(BlockEntity blockEntity, ChestType chestType);
}
