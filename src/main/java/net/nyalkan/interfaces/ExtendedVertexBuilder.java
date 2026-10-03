package net.nyalkan.interfaces;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface ExtendedVertexBuilder {

    static ExtendedVertexBuilder of(VertexConsumer vertexConsumer) {
        if (vertexConsumer instanceof ExtendedVertexBuilder) {
            return (ExtendedVertexBuilder) vertexConsumer;
        }

        return null;
    }

    default boolean canUseFastVertex() {
        return true;
    }

    default boolean canFastParticle() {
        return false;
    }

    default boolean canBulkWriteEntity() {
        return false;
    }

    // buffer sources set this
    default boolean cullsBackFaces() {
        return false;
    }

    default void setCullsBackFaces(boolean culls) {
    }

    default long reserveEntityVertices(int count) {
        throw new UnsupportedOperationException();
    }

    default VertexFormat vertexFormat() {
        return null;
    }

    default TextureAtlasSprite bulkSprite() {
        return null;
    }

    default int bulkVertexCount() {
        return -1;
    }

    default long bulkWriteEnd() {
        return 0L;
    }

    void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal);

    // Used for particles
    default void vertex(float x, float y, float z, float u, float v, int packedColor, int light) {}
}
