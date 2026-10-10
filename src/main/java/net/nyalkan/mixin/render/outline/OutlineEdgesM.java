package net.nyalkan.mixin.render.outline;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class OutlineEdgesM {
    @Unique private static final int SLOTS = 64;
    @Unique private static final VoxelShape[] nyalkan$shapes = new VoxelShape[SLOTS];
    @Unique private static final double[][] nyalkan$edges = new double[SLOTS][];
    @Unique private static int nyalkan$next;

    @WrapOperation(method = "renderShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/shapes/VoxelShape;forAllEdges(Lnet/minecraft/world/phys/shapes/Shapes$DoubleLineConsumer;)V"))
    private static void nyalkan$cachedEdges(VoxelShape shape, Shapes.DoubleLineConsumer consumer, Operation<Void> original) {
        if (!RenderSystem.isOnRenderThread()) {
            original.call(shape, consumer);
            return;
        }

        double[] edges = null;
        for (int i = 0; i < SLOTS; i++) {
            if (nyalkan$shapes[i] == shape) {
                edges = nyalkan$edges[i];
                break;
            }
        }
        if (edges == null) {
            DoubleArrayList list = new DoubleArrayList();
            original.call(shape, (Shapes.DoubleLineConsumer) (x1, y1, z1, x2, y2, z2) -> {
                list.add(x1); list.add(y1); list.add(z1);
                list.add(x2); list.add(y2); list.add(z2);
            });
            edges = list.toDoubleArray();
            nyalkan$shapes[nyalkan$next] = shape;
            nyalkan$edges[nyalkan$next] = edges;
            nyalkan$next = (nyalkan$next + 1) % SLOTS;
        }

        for (int i = 0; i < edges.length; i += 6)
            consumer.consume(edges[i], edges[i + 1], edges[i + 2], edges[i + 3], edges[i + 4], edges[i + 5]);
    }
}
