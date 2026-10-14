package net.nyalkan.render.bake;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.nyalkan.render.vertex.TerrainRenderType;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BlockModelQuads {
    private static final int STRIDE = 8;
    private static final Direction[] FACES = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};

    private BlockModelQuads() {}

    public static BakedMesh mesh(BlockState state, PoseStack poseStack, int tint) {
        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create();
        Map<TerrainRenderType, List<BakedQuad>> layers = new EnumMap<>(TerrainRenderType.class);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = layers.computeIfAbsent(TerrainRenderType.get(renderType), type -> new ArrayList<>());
            for (Direction face : FACES) {
                random.setSeed(42L);
                for (BakedQuad quad : model.getQuads(state, face, random, ModelData.EMPTY, renderType))
                    quads.add(transform(quad, poseStack.last(), tint));
            }
        }
        layers.replaceAll((type, quads) -> List.copyOf(quads));
        return new BakedMesh(layers, false);
    }

    private static BakedQuad transform(BakedQuad quad, PoseStack.Pose pose, int tint) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        int[] vertices = quad.getVertices().clone();
        int color = quad.isTinted()
                ? FastColor.ARGB32.red(tint) << 24 | FastColor.ARGB32.green(tint) << 16 | FastColor.ARGB32.blue(tint) << 8 | 0xFF
                : -1;
        Vector3f position = new Vector3f();
        for (int i = 0; i < vertices.length; i += STRIDE) {
            matrix.transformPosition(Float.intBitsToFloat(vertices[i]), Float.intBitsToFloat(vertices[i + 1]), Float.intBitsToFloat(vertices[i + 2]), position);
            vertices[i] = Float.floatToRawIntBits(position.x);
            vertices[i + 1] = Float.floatToRawIntBits(position.y);
            vertices[i + 2] = Float.floatToRawIntBits(position.z);
            vertices[i + 3] = color;
        }
        Vector3f facing = normal.transform(new Vector3f(quad.getDirection().step()));
        Direction direction = Direction.getNearest(facing.x, facing.y, facing.z);
        return new BakedQuad(vertices, -1, direction, quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
    }
}
