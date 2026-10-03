package net.nyalkan.mixin.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//TODO move
@Mixin(SpriteCoordinateExpander.class)
public class SpriteCoordinateExpanderM implements ExtendedVertexBuilder {
    @Shadow @Final private TextureAtlasSprite sprite;

    @Unique
    private ExtendedVertexBuilder extDelegate;
    @Unique
    private boolean canUseFastVertex = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void getExtBuilder(VertexConsumer vertexConsumer, TextureAtlasSprite textureAtlasSprite, CallbackInfo ci) {
        if (vertexConsumer instanceof ExtendedVertexBuilder) {
            this.extDelegate = (ExtendedVertexBuilder) vertexConsumer;
            this.canUseFastVertex = true;
        }
    }

    @Override
    public boolean canUseFastVertex() {
        return this.canUseFastVertex;
    }

    @Override
    public boolean canBulkWriteEntity() {
        return this.canUseFastVertex && this.extDelegate.canBulkWriteEntity() && this.extDelegate.bulkSprite() == null;
    }

    @Override
    public long reserveEntityVertices(int count) {
        return this.extDelegate.reserveEntityVertices(count);
    }

    @Override
    public TextureAtlasSprite bulkSprite() {
        return this.sprite;
    }

    @Override
    public void vertex(float x, float y, float z, int packedColor, float u, float v, int overlay, int light, int packedNormal) {
        this.extDelegate.vertex(x, y, z, packedColor, this.sprite.getU(u), this.sprite.getV(v), overlay, light, packedNormal);
    }
}
