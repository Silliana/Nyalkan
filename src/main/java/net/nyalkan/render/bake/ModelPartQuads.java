package net.nyalkan.render.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ModelPartQuads {
    private ModelPartQuads() {}

    @Nullable
    public static TextureAtlasSprite blockSprite(ResourceLocation texture) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS).getSprite(texture);
        return sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation()) ? null : sprite;
    }

    public static List<BakedQuad> bake(ModelLayerLocation layer, PoseStack poseStack, TextureAtlasSprite sprite) {
        Collector collector = new Collector();
        Minecraft.getInstance().getEntityModels().bakeLayer(layer).render(poseStack, collector, 0, OverlayTexture.NO_OVERLAY);
        return collector.build(sprite);
    }

    private static final class Collector implements VertexConsumer {
        private static final int FLOATS = 8;
        private final FloatArrayList data = new FloatArrayList();

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.data.add(x);
            this.data.add(y);
            this.data.add(z);
            for (int i = 3; i < FLOATS; i++)
                this.data.add(0.0F);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            int base = this.data.size() - FLOATS;
            this.data.set(base + 3, u);
            this.data.set(base + 4, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            int base = this.data.size() - FLOATS;
            this.data.set(base + 5, x);
            this.data.set(base + 6, y);
            this.data.set(base + 7, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            return this;
        }

        List<BakedQuad> build(TextureAtlasSprite sprite) {
            int vertexCount = this.data.size() / FLOATS;
            List<BakedQuad> quads = new ArrayList<>(vertexCount / 4);
            for (int q = 0; q + 4 <= vertexCount; q += 4) {
                int[] vertices = new int[32];
                int first = q * FLOATS;
                for (int i = 0; i < 4; i++) {
                    int src = first + i * FLOATS;
                    int dst = i * 8;
                    vertices[dst] = Float.floatToRawIntBits(this.data.getFloat(src));
                    vertices[dst + 1] = Float.floatToRawIntBits(this.data.getFloat(src + 1));
                    vertices[dst + 2] = Float.floatToRawIntBits(this.data.getFloat(src + 2));
                    vertices[dst + 3] = -1;
                    vertices[dst + 4] = Float.floatToRawIntBits(sprite.getU(this.data.getFloat(src + 3)));
                    vertices[dst + 5] = Float.floatToRawIntBits(sprite.getV(this.data.getFloat(src + 4)));
                }
                Direction direction = Direction.getNearest(this.data.getFloat(first + 5), this.data.getFloat(first + 6), this.data.getFloat(first + 7));
                quads.add(new BakedQuad(vertices, -1, direction, sprite, true));
            }
            return List.copyOf(quads);
        }
    }
}
