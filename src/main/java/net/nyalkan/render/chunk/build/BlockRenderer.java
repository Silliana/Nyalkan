package net.nyalkan.render.chunk.build;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.nyalkan.render.chunk.build.light.LightPipeline;
import net.nyalkan.render.chunk.build.light.data.QuadLightData;
import net.nyalkan.render.chunk.build.thread.BuilderResources;
import net.nyalkan.render.model.quad.QuadUtils;
import net.nyalkan.render.model.quad.QuadView;
import net.nyalkan.render.vertex.TerrainBufferBuilder;
import net.nyalkan.render.vertex.VertexUtil;
import net.nyalkan.vulkan.util.ColorUtil;
import org.joml.Vector3f;

import java.util.List;

public class BlockRenderer {

    static final Direction[] DIRECTIONS = Direction.values();
    private static final int OCCLUSION_CACHE_SIZE = 2048;
    private static final byte UNKNOWN_FACE = 127;
    private static BlockColors blockColors;

    RandomSource randomSource = RandomSource.createNewThreadLocalInstance();

    Vector3f pos;
    BlockPos blockPos;
    BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

    BuilderResources resources;

    BlockState blockState;

    // set by prepareBlock
    BakedModel currentModel;
    ModelData currentModelData;
    long currentSeed;
    private int facesChecked;
    private int facesVisible;

    public void setResources(BuilderResources resources) {
        this.resources = resources;
    }

    final Object2ByteLinkedOpenHashMap<Block.BlockStatePairKey> occlusionCache = new Object2ByteLinkedOpenHashMap<>(OCCLUSION_CACHE_SIZE, 0.25F) {
        protected void rehash(int i) {
        }
    };

    public BlockRenderer() {
        occlusionCache.defaultReturnValue(UNKNOWN_FACE);
    }

    public static void setBlockColors(BlockColors blockColors) {
        BlockRenderer.blockColors = blockColors;
    }

    public ChunkRenderTypeSet prepareBlock(BlockState blockState, BlockPos blockPos, Vector3f pos) {
        this.pos = pos;
        this.blockPos = blockPos;
        this.blockState = blockState;
        this.currentSeed = blockState.getSeed(blockPos);
        this.facesChecked = 0;
        this.facesVisible = 0;

        this.currentModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(blockState);

        Vec3 offset = blockState.getOffset(resources.region, blockPos);
        pos.add((float) offset.x, (float) offset.y, (float) offset.z);

        // main thread snapshot
        ModelData modelData = resources.region.getModelData(blockPos);
        // connected textures need this
        this.currentModelData = this.currentModel.getModelData(resources.region, blockPos, blockState, modelData);

        randomSource.setSeed(currentSeed);
        ChunkRenderTypeSet types = this.currentModel.getRenderTypes(blockState, randomSource, currentModelData);
        // some models return none
        if (types.isEmpty()) {
            types = ChunkRenderTypeSet.of(ItemBlockRenderTypes.getChunkRenderType(blockState));
        }
        return types;
    }

    public void renderBlockPass(TerrainBufferBuilder bufferBuilder, RenderType renderType) {
        BakedModel bakedModel = this.currentModel;
        ModelData modelData = this.currentModelData;

        LightPipeline lightPipeline = this.lightPipeline(bakedModel.useAmbientOcclusion());

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < DIRECTIONS.length; ++i) {
            Direction direction = DIRECTIONS[i];

            randomSource.setSeed(currentSeed);
            List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, randomSource, modelData, renderType);

            if (!quads.isEmpty() && this.isFaceVisible(i, direction))
                renderModelFace(bufferBuilder, quads, lightPipeline, direction);
        }

        randomSource.setSeed(currentSeed);
        List<BakedQuad> quads = bakedModel.getQuads(blockState, null, randomSource, modelData, renderType);
        if (!quads.isEmpty()) {
            renderModelFace(bufferBuilder, quads, lightPipeline, null);
        }
    }

    // emissive blocks stay flat
    private LightPipeline lightPipeline(boolean modelAO) {
        boolean smooth = modelAO && Minecraft.useAmbientOcclusion() && blockState.getLightEmission() == 0;
        return smooth ? resources.smoothLightPipeline : resources.flatLightPipeline;
    }

    // shared across render types
    private boolean isFaceVisible(int index, Direction direction) {
        int bit = 1 << index;
        if ((this.facesChecked & bit) == 0) {
            this.facesChecked |= bit;
            mutableBlockPos.setWithOffset(blockPos, direction);
            if (shouldRenderFace(blockState, direction, mutableBlockPos))
                this.facesVisible |= bit;
        }
        return (this.facesVisible & bit) != 0;
    }

    private void renderModelFace(TerrainBufferBuilder bufferBuilder, List<BakedQuad> quads, LightPipeline lightPipeline, Direction cullFace) {
        QuadLightData quadLightData = resources.quadLightData;

        for (int i = 0; i < quads.size(); ++i) {
            BakedQuad bakedQuad = quads.get(i);
            QuadView quadView = (QuadView) bakedQuad;
            LightPipeline quadLightPipeline = bakedQuad.hasAmbientOcclusion() ? lightPipeline : resources.flatLightPipeline;
            quadLightPipeline.calculate(quadView, blockPos, quadLightData, cullFace, bakedQuad.getDirection(), bakedQuad.isShade());
            putQuadData(bufferBuilder, quadView, quadLightData);
        }
    }

    private void putQuadData(TerrainBufferBuilder bufferBuilder, QuadView quadView, QuadLightData quadLightData) {
        float r, g, b;
        if (quadView.isTinted()) {
            int color = blockColors.getColor(blockState, resources.region, blockPos, quadView.getColorIndex());
            r = ColorUtil.ARGB.unpackR(color);
            g = ColorUtil.ARGB.unpackG(color);
            b = ColorUtil.ARGB.unpackB(color);
        } else {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
        }

        writeQuad(bufferBuilder, pos, quadView, quadLightData, r, g, b);
    }

    private static void writeQuad(TerrainBufferBuilder bufferBuilder, Vector3f pos, QuadView quad, QuadLightData quadLightData, float red, float green, float blue) {
        Vec3i normal = quad.getFacingDirection().getNormal();
        int packedNormal = VertexUtil.packNormal(normal.getX(), normal.getY(), normal.getZ());

        float[] brightnessArr = quadLightData.br;
        int[] lights = quadLightData.lm;

        // Rotate triangles if needed to fix AO anisotropy
        int idx = QuadUtils.getIterationStartIdx(brightnessArr, lights);

        bufferBuilder.ensureCapacity();

        for (byte i = 0; i < 4; ++i) {
            final float x = pos.x() + quad.getX(idx);
            final float y = pos.y() + quad.getY(idx);
            final float z = pos.z() + quad.getZ(idx);

            final float r, g, b;
            final float quadR, quadG, quadB;

            final int quadColor = quad.getColor(idx);
            quadR = ColorUtil.RGBA.unpackR(quadColor);
            quadG = ColorUtil.RGBA.unpackG(quadColor);
            quadB = ColorUtil.RGBA.unpackB(quadColor);

            final float brightness = brightnessArr[idx];
            r = quadR * brightness * red;
            g = quadG * brightness * green;
            b = quadB * brightness * blue;

            final int color = ColorUtil.RGBA.pack(r, g, b, 1.0f);
            final int light = lights[idx];
            final float u = quad.getU(idx);
            final float v = quad.getV(idx);

            bufferBuilder.vertex(x, y, z, color, u, v, light, packedNormal);

            idx = (idx + 1) & 0b11;
        }

    }

    public boolean shouldRenderFace(BlockState blockState, Direction direction, BlockPos adjPos) {
        BlockGetter blockGetter = resources.region;
        BlockState adjBlockState = blockGetter.getBlockState(adjPos);

        if (blockState.skipRendering(adjBlockState, direction)) {
            return false;
        }

        if (adjBlockState.canOcclude()) {
            VoxelShape shape = blockState.getFaceOcclusionShape(blockGetter, blockPos, direction);

            if (shape.isEmpty())
                return true;

            VoxelShape adjShape = adjBlockState.getFaceOcclusionShape(blockGetter, adjPos, direction.getOpposite());

            if (adjShape.isEmpty())
                return true;

            if (shape == Shapes.block() && adjShape == Shapes.block()) {
                return false;
            }

            Block.BlockStatePairKey blockStatePairKey = new Block.BlockStatePairKey(blockState, adjBlockState, direction);

            byte b = occlusionCache.getAndMoveToFirst(blockStatePairKey);
            if (b != UNKNOWN_FACE) {
                return b != 0;
            } else {
                boolean bl = Shapes.joinIsNotEmpty(shape, adjShape, BooleanOp.ONLY_FIRST);

                if (occlusionCache.size() == OCCLUSION_CACHE_SIZE) {
                    occlusionCache.removeLastByte();
                }

                occlusionCache.putAndMoveToFirst(blockStatePairKey, (byte) (bl ? 1 : 0));
                return bl;
            }
        }

        return true;
    }
}

