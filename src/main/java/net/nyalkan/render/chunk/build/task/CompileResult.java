package net.nyalkan.render.chunk.build.task;

import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.nyalkan.interfaces.VisibilitySetExtended;
import net.nyalkan.render.chunk.RenderSection;
import net.nyalkan.render.chunk.build.UploadBuffer;
import net.nyalkan.render.vertex.QuadSorter;
import net.nyalkan.render.vertex.TerrainRenderType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class CompileResult {
    public final RenderSection renderSection;
    public final boolean fullUpdate;
    public final int generation;

    final List<BlockEntity> globalBlockEntities = new ArrayList<>();
    final List<BlockEntity> blockEntities = new ArrayList<>();
    final List<BlockEntity> bakedBlockEntities = new ArrayList<>();
    final List<BlockEntity> partialBlockEntities = new ArrayList<>();
    public final EnumMap<TerrainRenderType, UploadBuffer> renderedLayers = new EnumMap<>(TerrainRenderType.class);

    VisibilitySet visibilitySet;
    QuadSorter.SortState transparencyState;
    CompiledSection compiledSection;

    CompileResult(RenderSection renderSection, boolean fullUpdate) {
        this(renderSection, fullUpdate, -1);
    }

    CompileResult(RenderSection renderSection, boolean fullUpdate, int generation) {
        this.renderSection = renderSection;
        this.fullUpdate = fullUpdate;
        this.generation = generation;
    }

    public void updateSection() {
        this.renderSection.updateGlobalBlockEntities(globalBlockEntities);
        this.renderSection.setCompiledSection(compiledSection);
        this.renderSection.setVisibility(((VisibilitySetExtended)visibilitySet).getVisibility());
        this.renderSection.setCompletelyEmpty(compiledSection.isCompletelyEmpty);
        this.renderSection.setContainsBlockEntities(!blockEntities.isEmpty());
    }
}
