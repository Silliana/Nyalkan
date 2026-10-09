package net.nyalkan.render.vertex;

import org.joml.Matrix3f;
import org.joml.Vector3f;

// neoforge baked light normals
public final class BakedQuadData {
    private BakedQuadData() {}

    public static int light(int light, int packed) {
        int block = Math.max(light & 0xFFFF, packed & 0xFFFF);
        int sky = Math.max(light >> 16 & 0xFFFF, packed >>> 16 & 0xFFFF);
        return block | sky << 16;
    }

    public static void normal(Vector3f normal, int packed, Matrix3f matrix) {
        byte x = (byte) packed, y = (byte) (packed >> 8), z = (byte) (packed >> 16);
        if (x != 0 || y != 0 || z != 0)
            normal.set(x / 127.0f, y / 127.0f, z / 127.0f).mul(matrix);
    }
}
