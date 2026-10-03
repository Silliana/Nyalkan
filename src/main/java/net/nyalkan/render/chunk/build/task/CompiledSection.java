package net.nyalkan.render.chunk.build.task;

import com.google.common.collect.Lists;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nyalkan.render.vertex.QuadSorter;
import net.nyalkan.render.vertex.TerrainBufferBuilder;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Set;

public class CompiledSection {
    public static final CompiledSection UNCOMPILED = new CompiledSection();

    boolean isCompletelyEmpty = false;
    final List<BlockEntity> blockEntities = Lists.newArrayList();
    final List<BlockEntity> bakedBlockEntities = Lists.newArrayList();
    Set<BlockEntity> partialBlockEntities = Set.of();
    @Nullable QuadSorter.SortState transparencyState;
    @Nullable TextureAtlasSprite[] animatedSprites;

    public boolean hasTransparencyState() {
        return this.transparencyState != null;
    }

    @Nullable
    public TextureAtlasSprite[] getAnimatedSprites() {
        return this.animatedSprites;
    }

    public List<BlockEntity> getBlockEntities() {
        return this.blockEntities;
    }

    public List<BlockEntity> getBakedBlockEntities() {
        return this.bakedBlockEntities;
    }

    public Set<BlockEntity> getPartialBlockEntities() {
        return this.partialBlockEntities;
    }
}


