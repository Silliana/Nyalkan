package net.nyalkan.mixin.render.entity.model;

import net.nyalkan.render.vertex.EntityVertex;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.nyalkan.interfaces.ExtendedModelPart;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.interfaces.ModelPartCubeMixed;
import net.nyalkan.render.model.BackfaceCull;
import net.nyalkan.render.model.CubeModel;
import net.nyalkan.render.model.ModelReplay;
import net.nyalkan.render.vertex.VertexUtil;
import net.nyalkan.vulkan.util.ColorUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Mixin(ModelPart.class)
public abstract class ModelPartM implements ExtendedModelPart {
    @Shadow @Final private List<ModelPart.Cube> cubes;

    @Shadow public float x;
    @Shadow public float y;
    @Shadow public float z;
    @Shadow public float xRot;
    @Shadow public float yRot;
    @Shadow public float zRot;
    @Shadow public float xScale;
    @Shadow public float yScale;
    @Shadow public float zScale;
    @Shadow public boolean visible;
    @Shadow public boolean skipDraw;
    @Shadow @Final private Map<String, ModelPart> children;

    @Unique private final float[] axisNormals = new float[18];
    @Unique private final int[] packedNormals = new int[6];
    @Unique private final long[] cornerXY = new long[8];
    @Unique private final long[] cornerZW = new long[8];
    @Unique private final float[] cornerPos = new float[24];
    @Unique private final float[] faceNormals = new float[9];

    @Unique private List<ModelPart> nyalkan$allParts;
    @Unique private int nyalkan$allPartsChildren = -1;

    @Inject(method = "getAllParts", at = @At("HEAD"), cancellable = true)
    private void nyalkan$cachedAllParts(CallbackInfoReturnable<Stream<ModelPart>> cir) {
        List<ModelPart> parts = this.nyalkan$allParts;
        if (parts == null || this.nyalkan$allPartsChildren != this.children.size()) {
            ArrayList<ModelPart> list = new ArrayList<>();
            list.add((ModelPart) (Object) this);
            for (ModelPart child : this.children.values())
                child.getAllParts().forEach(list::add);
            parts = List.copyOf(list);
            this.nyalkan$allParts = parts;
            this.nyalkan$allPartsChildren = this.children.size();
        }
        cir.setReturnValue(parts.stream());
    }

    @Unique private int nyalkan$geometryChildren = -1;
    @Unique private boolean nyalkan$geometry;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V", at = @At("HEAD"), cancellable = true)
    private void nyalkan$skipEmpty(PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color, CallbackInfo ci) {
        if (!this.nyalkan$hasGeometry())
            ci.cancel();
    }

    @Override
    public boolean nyalkan$hasGeometry() {
        if (this.nyalkan$geometryChildren != this.children.size()) {
            boolean geometry = !this.cubes.isEmpty();
            for (ModelPart child : this.children.values()) {
                if (geometry)
                    break;
                geometry = ((ExtendedModelPart) (Object) child).nyalkan$hasGeometry();
            }
            this.nyalkan$geometry = geometry;
            this.nyalkan$geometryChildren = this.children.size();
        }
        return this.nyalkan$geometry;
    }

    @Inject(method = "compile", at = @At("HEAD"), cancellable = true)
    private void injCompile(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color, CallbackInfo ci) {
        this.renderCubes(pose, vertexConsumer, light, overlay, color);
        ci.cancel();
    }

    @Inject(method = "translateAndRotate", at = @At("HEAD"), cancellable = true)
    private void nyalkan$translateAndRotate(PoseStack poseStack, CallbackInfo ci) {
        if (this.x != 0.0F || this.y != 0.0F || this.z != 0.0F)
            poseStack.translate(this.x / 16.0F, this.y / 16.0F, this.z / 16.0F);

        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            PoseStack.Pose pose = poseStack.last();
            pose.pose().rotateZYX(this.zRot, this.yRot, this.xRot);
            pose.normal().rotateZYX(this.zRot, this.yRot, this.xRot);
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }

        ci.cancel();
    }

    @Unique
    public void renderCubes(PoseStack.Pose pose, VertexConsumer vertexConsumer, int light, int overlay, int color) {
        Matrix4f matrix4f = pose.pose();
        this.computeAxisNormals(pose.normal());

        float[] normals = this.axisNormals;
        ExtendedVertexBuilder vertexBuilder = ExtendedVertexBuilder.of(vertexConsumer);

        if (vertexBuilder != null && vertexBuilder.canBulkWriteEntity()) {
            this.writeCubesBulk(matrix4f, vertexBuilder, ColorUtil.RGBA.fromArgb32(color), light, overlay);
        }
        else if (vertexBuilder != null && vertexBuilder.canUseFastVertex()) {
            color = ColorUtil.RGBA.fromArgb32(color);

            for (ModelPart.Cube cube : this.cubes) {
                CubeModel cubeModel = ((ModelPartCubeMixed) cube).getCubeModel();
                ModelPart.Polygon[] polygons = cubeModel.getPolygons();
                int[] normalAxis = cubeModel.getNormalAxis();

                cubeModel.transformVertices(matrix4f);

                for (int p = 0; p < polygons.length; p++) {
                    int packedNormal = this.packedNormals[normalAxis[p]];

                    for (ModelPart.Vertex vertex : polygons[p].vertices) {
                        Vector3f pos = vertex.pos;
                        vertexBuilder.vertex(pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, overlay, light, packedNormal);
                    }
                }
            }
        }
        else {
            for (ModelPart.Cube cube : this.cubes) {
                CubeModel cubeModel = ((ModelPartCubeMixed) cube).getCubeModel();
                ModelPart.Polygon[] polygons = cubeModel.getPolygons();
                int[] normalAxis = cubeModel.getNormalAxis();

                cubeModel.transformVertices(matrix4f);

                for (int p = 0; p < polygons.length; p++) {
                    int n = normalAxis[p] * 3;

                    for (ModelPart.Vertex vertex : polygons[p].vertices) {
                        Vector3f pos = vertex.pos;
                        vertexConsumer.addVertex(pos.x(), pos.y(), pos.z(), color, vertex.u, vertex.v, overlay, light,
                                                 normals[n], normals[n + 1], normals[n + 2]);
                    }
                }
            }
        }
    }

    @Unique
    private void writeCubesBulk(Matrix4f m, ExtendedVertexBuilder vertexBuilder, int color, int light, int overlay) {
        TextureAtlasSprite sprite = vertexBuilder.bulkSprite();
        long overlayLight = CubeModel.pack(overlay, light);
        long[] cornerXY = this.cornerXY;
        long[] cornerZW = this.cornerZW;
        boolean cull = BackfaceCull.enabled(vertexBuilder);

        for (ModelPart.Cube cube : this.cubes) {
            CubeModel cubeModel = ((ModelPartCubeMixed) cube).getCubeModel();
            int vertexCount = cubeModel.getVertexCount();
            if (vertexCount == 0)
                continue;

            this.computeCorners(m, cubeModel, color);

            int[] normalAxis = cubeModel.getNormalAxis();
            int[] faceCorners = cubeModel.getFaceCorners();
            long[] faceUvs = cubeModel.getFaceUvs();

            int visible = -1;
            int written = vertexCount;
            if (cull) {
                visible = this.nyalkan$frontFaces(faceCorners, cubeModel.getFaceWinding(), vertexCount >> 2);
                written = Integer.bitCount(visible) << 2;
                if (written == 0)
                    continue;
            }

            long ptr = vertexBuilder.reserveEntityVertices(written);
            ModelReplay.bulkVertices += written;
            for (int i = 0; i < vertexCount; i++) {
                if ((visible >>> (i >> 2) & 1) == 0)
                    continue;
                int corner = faceCorners[i];
                long uv = faceUvs[i];

                if (sprite != null) {
                    float u = sprite.getU(Float.intBitsToFloat((int) uv));
                    float v = sprite.getV(Float.intBitsToFloat((int) (uv >>> 32)));
                    uv = CubeModel.pack(Float.floatToRawIntBits(u), Float.floatToRawIntBits(v));
                }

                MemoryUtil.memPutLong(ptr, cornerXY[corner]);
                MemoryUtil.memPutLong(ptr + EntityVertex.Z, cornerZW[corner]);
                MemoryUtil.memPutLong(ptr + EntityVertex.U, uv);
                MemoryUtil.memPutLong(ptr + EntityVertex.OVERLAY, overlayLight);
                MemoryUtil.memPutInt(ptr + EntityVertex.NORMAL, this.packedNormals[normalAxis[i >> 2]]);
                ptr += EntityVertex.STRIDE;
            }
        }
    }

    @Unique
    private int nyalkan$frontFaces(int[] faceCorners, int[] winding, int faces) {
        float[] c = this.cornerPos;
        float ax = c[3] - c[0], ay = c[4] - c[1], az = c[5] - c[2];
        float bx = c[9] - c[0], by = c[10] - c[1], bz = c[11] - c[2];
        float cx = c[12] - c[0], cy = c[13] - c[1], cz = c[14] - c[2];
        float[] n = this.faceNormals;
        n[0] = by * cz - bz * cy; n[1] = bz * cx - bx * cz; n[2] = bx * cy - by * cx;
        n[3] = cy * az - cz * ay; n[4] = cz * ax - cx * az; n[5] = cx * ay - cy * ax;
        n[6] = ay * bz - az * by; n[7] = az * bx - ax * bz; n[8] = ax * by - ay * bx;

        int mask = 0;
        for (int f = 0; f < faces; f++) {
            int w = winding[f];
            int a = faceCorners[f << 2] * 3;
            if (w < 0) {
                int b = faceCorners[(f << 2) + 1] * 3, d = faceCorners[(f << 2) + 2] * 3;
                if (BackfaceCull.frontFacing(c[a], c[a + 1], c[a + 2], c[b], c[b + 1], c[b + 2], c[d], c[d + 1], c[d + 2]))
                    mask |= 1 << f;
                continue;
            }
            int k = (w >> 1) * 3;
            float nx = n[k], ny = n[k + 1], nz = n[k + 2];
            float dot = nx * c[a] + ny * c[a + 1] + nz * c[a + 2];
            if ((w & 1) != 0)
                dot = -dot;
            if (nx * nx + ny * ny + nz * nz < 1.0e-12f || dot <= 0.0f)
                mask |= 1 << f;
        }
        return mask;
    }

    @Unique
    private void computeCorners(Matrix4f m, CubeModel cube, int color) {
        float ox = cube.originX, oy = cube.originY, oz = cube.originZ;

        float x0 = m.m00() * ox + m.m10() * oy + m.m20() * oz + m.m30();
        float y0 = m.m01() * ox + m.m11() * oy + m.m21() * oz + m.m31();
        float z0 = m.m02() * ox + m.m12() * oy + m.m22() * oz + m.m32();

        float axX = m.m00() * cube.sizeX, axY = m.m01() * cube.sizeX, axZ = m.m02() * cube.sizeX;
        float ayX = m.m10() * cube.sizeY, ayY = m.m11() * cube.sizeY, ayZ = m.m12() * cube.sizeY;
        float azX = m.m20() * cube.sizeZ, azY = m.m21() * cube.sizeZ, azZ = m.m22() * cube.sizeZ;

        this.setCorner(0, x0, y0, z0, color);
        this.setCorner(1, x0 + axX, y0 + axY, z0 + axZ, color);
        this.setCorner(2, x0 + axX + ayX, y0 + axY + ayY, z0 + axZ + ayZ, color);
        this.setCorner(3, x0 + ayX, y0 + ayY, z0 + ayZ, color);
        this.setCorner(4, x0 + azX, y0 + azY, z0 + azZ, color);
        this.setCorner(5, x0 + axX + azX, y0 + axY + azY, z0 + axZ + azZ, color);
        this.setCorner(6, x0 + axX + ayX + azX, y0 + axY + ayY + azY, z0 + axZ + ayZ + azZ, color);
        this.setCorner(7, x0 + ayX + azX, y0 + ayY + azY, z0 + ayZ + azZ, color);
    }

    @Unique
    private void setCorner(int corner, float x, float y, float z, int color) {
        this.cornerXY[corner] = CubeModel.pack(Float.floatToRawIntBits(x), Float.floatToRawIntBits(y));
        this.cornerZW[corner] = CubeModel.pack(Float.floatToRawIntBits(z), color);
        this.cornerPos[corner * 3] = x;
        this.cornerPos[corner * 3 + 1] = y;
        this.cornerPos[corner * 3 + 2] = z;
    }

    @Override
    public int nyalkan$stateSize() {
        int size = 11;
        for (ModelPart child : this.children.values())
            size += ((ExtendedModelPart) (Object) child).nyalkan$stateSize();
        return size;
    }

    @Override
    public int nyalkan$writeState(float[] dst, int offset) {
        dst[offset] = this.x;
        dst[offset + 1] = this.y;
        dst[offset + 2] = this.z;
        dst[offset + 3] = this.xRot;
        dst[offset + 4] = this.yRot;
        dst[offset + 5] = this.zRot;
        dst[offset + 6] = this.xScale;
        dst[offset + 7] = this.yScale;
        dst[offset + 8] = this.zScale;
        dst[offset + 9] = this.visible ? 1.0f : 0.0f;
        dst[offset + 10] = this.skipDraw ? 1.0f : 0.0f;
        offset += 11;

        for (ModelPart child : this.children.values())
            offset = ((ExtendedModelPart) (Object) child).nyalkan$writeState(dst, offset);
        return offset;
    }

    @Override
    public int nyalkan$readState(float[] src, int offset) {
        this.x = src[offset];
        this.y = src[offset + 1];
        this.z = src[offset + 2];
        this.xRot = src[offset + 3];
        this.yRot = src[offset + 4];
        this.zRot = src[offset + 5];
        this.xScale = src[offset + 6];
        this.yScale = src[offset + 7];
        this.zScale = src[offset + 8];
        this.visible = src[offset + 9] != 0.0f;
        this.skipDraw = src[offset + 10] != 0.0f;
        offset += 11;

        for (ModelPart child : this.children.values())
            offset = ((ExtendedModelPart) (Object) child).nyalkan$readState(src, offset);
        return offset;
    }

    @Unique
    private void computeAxisNormals(Matrix3f m) {
        this.setAxisNormal(0, m.m00(), m.m01(), m.m02());
        this.setAxisNormal(2, m.m10(), m.m11(), m.m12());
        this.setAxisNormal(4, m.m20(), m.m21(), m.m22());
    }

    @Unique
    private void setAxisNormal(int axis, float nx, float ny, float nz) {
        float invLength = 1.0f / (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        nx *= invLength;
        ny *= invLength;
        nz *= invLength;

        float[] normals = this.axisNormals;
        int i = axis * 3;
        normals[i] = nx;
        normals[i + 1] = ny;
        normals[i + 2] = nz;
        normals[i + 3] = -nx;
        normals[i + 4] = -ny;
        normals[i + 5] = -nz;

        this.packedNormals[axis] = VertexUtil.packNormal(nx, ny, nz);
        this.packedNormals[axis + 1] = VertexUtil.packNormal(-nx, -ny, -nz);
    }
}
