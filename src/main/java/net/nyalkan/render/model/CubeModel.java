package net.nyalkan.render.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

public class CubeModel {

    private ModelPart.Polygon[] polygons = new ModelPart.Polygon[6];
    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    private int[] normalAxis;

    public float originX, originY, originZ;
    public float sizeX, sizeY, sizeZ;
    private int[] faceCorners;
    private int[] faceWinding;
    private long[] faceUvs;

    Vector3f[] vertices;
    Vector3f[] transformed = new Vector3f[8];

    public void setVertices(int i, int j, float f, float g, float h, float k, float l, float m, float n, float o, float p, boolean bl, float q, float r, Set<Direction> set) {
        this.minX = f;
        this.minY = g;
        this.minZ = h;
        this.maxX = f + k;
        this.maxY = g + l;
        this.maxZ = h + m;
        this.polygons = new ModelPart.Polygon[set.size()];
        float s = maxX;
        float t = maxY;
        float u = maxZ;
        f -= n;
        g -= o;
        h -= p;
        s += n;
        t += o;
        u += p;
        if (bl) {
            float v = s;
            s = f;
            f = v;
        }

        this.vertices = new Vector3f[]{
                new Vector3f(f, g, h),
                new Vector3f(s, g, h),
                new Vector3f(s, t, h),
                new Vector3f(f, t, h),
                new Vector3f(f, g, u),
                new Vector3f(s, g, u),
                new Vector3f(s, t, u),
                new Vector3f(f, t, u)
        };

        for (int i1 = 0; i1 < 8; i1++) {
            //pre-divide all vertices once
            this.vertices[i1].div(16.0f);
            this.transformed[i1] = new Vector3f(0.0f);
        }

        ModelPart.Vertex vertex1 = new ModelPart.Vertex(transformed[0], 0.0F, 0.0F);
        ModelPart.Vertex vertex2 = new ModelPart.Vertex(transformed[1], 0.0F, 8.0F);
        ModelPart.Vertex vertex3 = new ModelPart.Vertex(transformed[2], 8.0F, 8.0F);
        ModelPart.Vertex vertex4 = new ModelPart.Vertex(transformed[3], 8.0F, 0.0F);
        ModelPart.Vertex vertex5 = new ModelPart.Vertex(transformed[4], 0.0F, 0.0F);
        ModelPart.Vertex vertex6 = new ModelPart.Vertex(transformed[5], 0.0F, 8.0F);
        ModelPart.Vertex vertex7 = new ModelPart.Vertex(transformed[6], 8.0F, 8.0F);
        ModelPart.Vertex vertex8 = new ModelPart.Vertex(transformed[7], 8.0F, 0.0F);

        float w = (float)i;
        float x = (float)i + m;
        float y = (float)i + m + k;
        float z = (float)i + m + k + k;
        float aa = (float)i + m + k + m;
        float ab = (float)i + m + k + m + k;
        float ac = (float)j;
        float ad = (float)j + m;
        float ae = (float)j + m + l;
        int idx = 0;
        if (set.contains(Direction.DOWN)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex6, vertex5, vertex1, vertex2}, x, ac, y, ad, q, r, bl, Direction.DOWN);
        }

        if (set.contains(Direction.UP)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex3, vertex4, vertex8, vertex7}, y, ad, z, ac, q, r, bl, Direction.UP);
        }

        if (set.contains(Direction.WEST)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex1, vertex5, vertex8, vertex4}, w, ad, x, ae, q, r, bl, Direction.WEST);
        }

        if (set.contains(Direction.NORTH)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex2, vertex1, vertex4, vertex3}, x, ad, y, ae, q, r, bl, Direction.NORTH);
        }

        if (set.contains(Direction.EAST)) {
            this.polygons[idx++] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex6, vertex2, vertex3, vertex7}, y, ad, aa, ae, q, r, bl, Direction.EAST);
        }

        if (set.contains(Direction.SOUTH)) {
            this.polygons[idx] = new ModelPart.Polygon(new ModelPart.Vertex[]{vertex5, vertex6, vertex7, vertex8}, aa, ad, ab, ae, q, r, bl, Direction.SOUTH);
        }

        this.normalAxis = new int[this.polygons.length];
        for (int face = 0; face < this.polygons.length; face++)
            this.normalAxis[face] = axisIndex(this.polygons[face].normal);

        Vector3f min = this.vertices[0];
        Vector3f max = this.vertices[6];
        this.originX = min.x;
        this.originY = min.y;
        this.originZ = min.z;
        this.sizeX = max.x - min.x;
        this.sizeY = max.y - min.y;
        this.sizeZ = max.z - min.z;

        this.faceCorners = new int[this.polygons.length * 4];
        this.faceUvs = new long[this.polygons.length * 4];
        for (int face = 0; face < this.polygons.length; face++) {
            ModelPart.Vertex[] faceVertices = this.polygons[face].vertices;
            for (int corner = 0; corner < 4; corner++) {
                ModelPart.Vertex vertex = faceVertices[corner];
                this.faceCorners[face * 4 + corner] = cornerIndex(vertex.pos);
                this.faceUvs[face * 4 + corner] = pack(Float.floatToRawIntBits(vertex.u), Float.floatToRawIntBits(vertex.v));
            }
        }

        this.faceWinding = new int[this.polygons.length];
        for (int face = 0; face < this.polygons.length; face++)
            this.faceWinding[face] = winding(this.faceCorners[face * 4], this.faceCorners[face * 4 + 1], this.faceCorners[face * 4 + 2]);
    }

    private static final int[][] UNIT_CORNERS = {{0, 0, 0}, {1, 0, 0}, {1, 1, 0}, {0, 1, 0}, {0, 0, 1}, {1, 0, 1}, {1, 1, 1}, {0, 1, 1}};

    // axis plus sign bit
    private static int winding(int a, int b, int c) {
        int[] pa = UNIT_CORNERS[a], pb = UNIT_CORNERS[b], pc = UNIT_CORNERS[c];
        int ux = pb[0] - pa[0], uy = pb[1] - pa[1], uz = pb[2] - pa[2];
        int vx = pc[0] - pa[0], vy = pc[1] - pa[1], vz = pc[2] - pa[2];
        int[] n = {uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx};
        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(n[axis]) == 1 && n[(axis + 1) % 3] == 0 && n[(axis + 2) % 3] == 0)
                return axis << 1 | (n[axis] < 0 ? 1 : 0);
        }
        return -1;
    }

    private int cornerIndex(Vector3f pos) {
        for (int i = 0; i < 8; i++) {
            if (this.transformed[i] == pos)
                return i;
        }
        throw new IllegalStateException("Polygon vertex is not a cube corner");
    }

    public static long pack(int low, int high) {
        return ((long) high << 32) | (low & 0xFFFFFFFFL);
    }

    public int[] getFaceCorners() { return this.faceCorners; }

    public long[] getFaceUvs() { return this.faceUvs; }

    public int[] getFaceWinding() { return this.faceWinding; }

    // +X -X +Y -Y +Z -Z
    private static int axisIndex(Vector3f normal) {
        if (normal.x != 0.0f)
            return normal.x > 0.0f ? 0 : 1;
        if (normal.y != 0.0f)
            return normal.y > 0.0f ? 2 : 3;
        return normal.z > 0.0f ? 4 : 5;
    }

    public int[] getNormalAxis() { return this.normalAxis; }

    public int getVertexCount() { return this.polygons.length * 4; }

    public void transformVertices(Matrix4f matrix) {
        //Transform original vertices and store them
        for(int i = 0; i < 8; ++i) {
            this.vertices[i].mulPosition(matrix, this.transformed[i]);
        }
    }

    public ModelPart.Polygon[] getPolygons() { return this.polygons; }
}
