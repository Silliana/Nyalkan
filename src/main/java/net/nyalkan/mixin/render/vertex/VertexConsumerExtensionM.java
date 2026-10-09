package net.nyalkan.mixin.render.vertex;

import net.nyalkan.render.vertex.EntityVertex;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Vec3i;
import net.neoforged.neoforge.client.extensions.IVertexConsumerExtension;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.render.vertex.BakedQuadData;
import net.nyalkan.render.vertex.VertexUtil;
import net.nyalkan.vulkan.util.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = IVertexConsumerExtension.class, remap = false)
public interface VertexConsumerExtensionM {
    /**
     * @author Nyalkan
     * @reason write quads in bulk
     */
    @Overwrite
    default void putBulkData(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, boolean readExistingColor) {
        VertexConsumer consumer = (VertexConsumer) this;
        int[] js = quad.getVertices();
        ExtendedVertexBuilder builder = ExtendedVertexBuilder.of(consumer);
        if (js.length != 32 || builder == null || !builder.canBulkWriteEntity() || builder.bulkSprite() != null) {
            int[] lights = {packedLight, packedLight, packedLight, packedLight};
            consumer.putBulkData(pose, quad, new float[]{1.0f, 1.0f, 1.0f, 1.0f}, red, green, blue, alpha, lights, packedOverlay, readExistingColor);
            return;
        }

        Matrix4f m = pose.pose();
        Matrix3f normalMatrix = pose.normal();
        Vec3i face = quad.getDirection().getNormal();
        Vector3f normal = new Vector3f(face.getX(), face.getY(), face.getZ()).mul(normalMatrix);
        int flatColor = ColorUtil.RGBA.pack(red, green, blue, alpha);

        long ptr = builder.reserveEntityVertices(4);
        for (int i = 0; i < 32; i += 8, ptr += EntityVertex.STRIDE) {
            float x = Float.intBitsToFloat(js[i]);
            float y = Float.intBitsToFloat(js[i + 1]);
            float z = Float.intBitsToFloat(js[i + 2]);
            int color = flatColor;
            if (readExistingColor) {
                int c = js[i + 3];
                color = ColorUtil.RGBA.pack(ColorUtil.RGBA.unpackR(c) * red, ColorUtil.RGBA.unpackG(c) * green,
                        ColorUtil.RGBA.unpackB(c) * blue, alpha);
            }
            BakedQuadData.normal(normal, js[i + 7], normalMatrix);
            int light = BakedQuadData.light(packedLight, js[i + 6]);

            MemoryUtil.memPutFloat(ptr, m.m00() * x + m.m10() * y + m.m20() * z + m.m30());
            MemoryUtil.memPutFloat(ptr + EntityVertex.Y, m.m01() * x + m.m11() * y + m.m21() * z + m.m31());
            MemoryUtil.memPutFloat(ptr + EntityVertex.Z, m.m02() * x + m.m12() * y + m.m22() * z + m.m32());
            MemoryUtil.memPutInt(ptr + EntityVertex.COLOR, color);
            MemoryUtil.memPutInt(ptr + EntityVertex.U, js[i + 4]);
            MemoryUtil.memPutInt(ptr + EntityVertex.V, js[i + 5]);
            MemoryUtil.memPutLong(ptr + EntityVertex.OVERLAY, (packedOverlay & 0xFFFFFFFFL) | ((long) light << 32));
            MemoryUtil.memPutInt(ptr + EntityVertex.NORMAL, VertexUtil.packNormal(Math.max(-1.0f, Math.min(1.0f, normal.x())),
                    Math.max(-1.0f, Math.min(1.0f, normal.y())), Math.max(-1.0f, Math.min(1.0f, normal.z()))));
        }
    }
}
