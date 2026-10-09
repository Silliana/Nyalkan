package net.nyalkan.render.model;

import com.mojang.blaze3d.systems.RenderSystem;
import net.nyalkan.Initializer;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import org.joml.Matrix4f;
import org.joml.Vector4f;

public final class BackfaceCull {
    private BackfaceCull() {}

    public static boolean enabled(ExtendedVertexBuilder builder) {
        return Initializer.CONFIG.entityBackfaceCulling && builder.cullsBackFaces() && worldView();
    }

    private static boolean worldView() {
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Matrix4f modelView = RenderSystem.getModelViewMatrix();
        return projection.m33() == 0.0f && modelView.m30() == 0.0f && modelView.m31() == 0.0f && modelView.m32() == 0.0f;
    }

    // camera sits at origin
    public static boolean frontFacing(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz) {
        float e1x = bx - ax, e1y = by - ay, e1z = bz - az;
        float e2x = cx - ax, e2y = cy - ay, e2z = cz - az;
        float nx = e1y * e2z - e1z * e2y, ny = e1z * e2x - e1x * e2z, nz = e1x * e2y - e1y * e2x;
        float area = nx * nx + ny * ny + nz * nz;
        return area < 1.0e-12f || -(nx * ax + ny * ay + nz * az) >= 0.0f;
    }

    private static final Matrix4f QUAD_MVP = new Matrix4f();
    private static final Vector4f CORNER = new Vector4f();
    private static final float[] SCREEN = new float[8];
    private static boolean quadPrepared;

    public static void prepareQuads(Matrix4f pose) {
        quadPrepared = RenderSystem.isOnRenderThread();
        if (quadPrepared)
            RenderSystem.getProjectionMatrix().mul(RenderSystem.getModelViewMatrix(), QUAD_MVP).mul(pose);
    }

    // screen winding like rasterizer
    public static boolean quadVisible(ExtendedVertexBuilder builder, int[] vertices) {
        if (!quadPrepared || !Initializer.CONFIG.entityBackfaceCulling || !RenderSystem.isOnRenderThread() || vertices.length < 32)
            return true;
        int stride = vertices.length / 4;
        for (int i = 0; i < 4; i++) {
            int o = i * stride;
            QUAD_MVP.transform(Float.intBitsToFloat(vertices[o]), Float.intBitsToFloat(vertices[o + 1]), Float.intBitsToFloat(vertices[o + 2]), 1.0f, CORNER);
            if (CORNER.w <= 1.0e-6f)
                return true;
            SCREEN[i * 2] = CORNER.x / CORNER.w;
            SCREEN[i * 2 + 1] = CORNER.y / CORNER.w;
        }
        float first = area(0, 1, 2), second = area(0, 2, 3);
        if (first > 1.0e-10f || second > 1.0e-10f)
            return true;
        boolean flat = Math.abs(first) <= 1.0e-10f && Math.abs(second) <= 1.0e-10f;
        return !flat && !builder.cullsBackFaces();
    }

    private static float area(int a, int b, int c) {
        float[] p = SCREEN;
        return (p[b * 2] - p[a * 2]) * (p[c * 2 + 1] - p[a * 2 + 1]) - (p[c * 2] - p[a * 2]) * (p[b * 2 + 1] - p[a * 2 + 1]);
    }
}
