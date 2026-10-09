package net.nyalkan.mixin.compat.geckolib;

import net.nyalkan.render.vertex.EntityVertex;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.render.model.BackfaceCull;
import net.nyalkan.render.vertex.VertexUtil;
import net.nyalkan.vulkan.util.ColorUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import software.bernie.geckolib.cache.object.GeoQuad;
import software.bernie.geckolib.cache.object.GeoVertex;

@Pseudo
@Mixin(targets = "software.bernie.geckolib.renderer.GeoRenderer", remap = false)
public interface GeoRendererM {
    /**
     * @author Nyalkan
     * @reason write quads in bulk
     */
    @Overwrite
    default void createVerticesOfQuad(GeoQuad quad, Matrix4f poseState, Vector3f normal, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
        GeoVertex[] vertices = quad.vertices();
        ExtendedVertexBuilder builder = ExtendedVertexBuilder.of(buffer);
        if (vertices.length == 4 && builder != null && builder.canBulkWriteEntity() && builder.bulkSprite() == null) {
            Vector3f v0 = vertices[0].position(), v1 = vertices[1].position(), v2 = vertices[2].position(), v3 = vertices[3].position();
            Matrix4f m = poseState;
            float x0 = m.m00() * v0.x() + m.m10() * v0.y() + m.m20() * v0.z() + m.m30();
            float y0 = m.m01() * v0.x() + m.m11() * v0.y() + m.m21() * v0.z() + m.m31();
            float z0 = m.m02() * v0.x() + m.m12() * v0.y() + m.m22() * v0.z() + m.m32();
            float x1 = m.m00() * v1.x() + m.m10() * v1.y() + m.m20() * v1.z() + m.m30();
            float y1 = m.m01() * v1.x() + m.m11() * v1.y() + m.m21() * v1.z() + m.m31();
            float z1 = m.m02() * v1.x() + m.m12() * v1.y() + m.m22() * v1.z() + m.m32();
            float x2 = m.m00() * v2.x() + m.m10() * v2.y() + m.m20() * v2.z() + m.m30();
            float y2 = m.m01() * v2.x() + m.m11() * v2.y() + m.m21() * v2.z() + m.m31();
            float z2 = m.m02() * v2.x() + m.m12() * v2.y() + m.m22() * v2.z() + m.m32();
            if (BackfaceCull.enabled(builder) && !BackfaceCull.frontFacing(x0, y0, z0, x1, y1, z1, x2, y2, z2))
                return;
            float x3 = m.m00() * v3.x() + m.m10() * v3.y() + m.m20() * v3.z() + m.m30();
            float y3 = m.m01() * v3.x() + m.m11() * v3.y() + m.m21() * v3.z() + m.m31();
            float z3 = m.m02() * v3.x() + m.m12() * v3.y() + m.m22() * v3.z() + m.m32();

            int color = ColorUtil.RGBA.fromArgb32(colour);
            int packedNormal = VertexUtil.packNormal(Math.max(-1.0f, Math.min(1.0f, normal.x())),
                    Math.max(-1.0f, Math.min(1.0f, normal.y())), Math.max(-1.0f, Math.min(1.0f, normal.z())));
            long ptr = builder.reserveEntityVertices(4);
            long overlayLight = (packedOverlay & 0xFFFFFFFFL) | ((long) packedLight << 32);
            MemoryUtil.memPutFloat(ptr, x0); MemoryUtil.memPutFloat(ptr + EntityVertex.Y, y0); MemoryUtil.memPutFloat(ptr + EntityVertex.Z, z0);
            MemoryUtil.memPutFloat(ptr + EntityVertex.STRIDE, x1); MemoryUtil.memPutFloat(ptr + (EntityVertex.STRIDE + EntityVertex.Y), y1); MemoryUtil.memPutFloat(ptr + (EntityVertex.STRIDE + EntityVertex.Z), z1);
            MemoryUtil.memPutFloat(ptr + 2 * EntityVertex.STRIDE, x2); MemoryUtil.memPutFloat(ptr + (2 * EntityVertex.STRIDE + EntityVertex.Y), y2); MemoryUtil.memPutFloat(ptr + (2 * EntityVertex.STRIDE + EntityVertex.Z), z2);
            MemoryUtil.memPutFloat(ptr + 3 * EntityVertex.STRIDE, x3); MemoryUtil.memPutFloat(ptr + (3 * EntityVertex.STRIDE + EntityVertex.Y), y3); MemoryUtil.memPutFloat(ptr + (3 * EntityVertex.STRIDE + EntityVertex.Z), z3);
            for (int i = 0; i < 4; i++, ptr += EntityVertex.STRIDE) {
                MemoryUtil.memPutInt(ptr + EntityVertex.COLOR, color);
                MemoryUtil.memPutFloat(ptr + EntityVertex.U, vertices[i].texU());
                MemoryUtil.memPutFloat(ptr + EntityVertex.V, vertices[i].texV());
                MemoryUtil.memPutLong(ptr + EntityVertex.OVERLAY, overlayLight);
                MemoryUtil.memPutInt(ptr + EntityVertex.NORMAL, packedNormal);
            }
            return;
        }

        for (GeoVertex vertex : vertices) {
            Vector3f position = vertex.position();
            Vector4f vector4f = poseState.transform(new Vector4f(position.x(), position.y(), position.z(), 1.0f));
            buffer.addVertex(vector4f.x(), vector4f.y(), vector4f.z(), colour, vertex.texU(), vertex.texV(),
                    packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
        }
    }
}
