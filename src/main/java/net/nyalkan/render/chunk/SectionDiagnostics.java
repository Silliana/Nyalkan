package net.nyalkan.render.chunk;

import net.minecraft.client.Camera;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.nyalkan.Initializer;
import net.nyalkan.render.chunk.build.task.CompiledSection;

// temporary missing section report
final class SectionDiagnostics {
    static final boolean ENABLED = Boolean.getBoolean("nyalkan.sectionDiag")
            || java.nio.file.Files.exists(java.nio.file.Path.of("nyalkan-section-diag"));
    private static long last;

    private SectionDiagnostics() {}

    static void dump(WorldRenderer renderer, Camera camera) {
        long now = System.currentTimeMillis();
        if (now - last < 5000 || renderer.getSectionGrid() == null || WorldRenderer.getLevel() == null)
            return;
        last = now;

        var level = WorldRenderer.getLevel();
        int cx = SectionPos.blockToSectionCoord(camera.getBlockPosition().getX());
        int cy = SectionPos.blockToSectionCoord(camera.getBlockPosition().getY());
        int cz = SectionPos.blockToSectionCoord(camera.getBlockPosition().getZ());
        StringBuilder out = new StringBuilder();
        int missing = 0;
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                for (int y = cy - 2; y <= cy + 1; y++) {
                    RenderSection section = renderer.getSectionGrid().getSectionAtSectionPos(x, y - level.getMinSection(), z);
                    if (section == null)
                        continue;
                    boolean hasBlocks = false;
                    if (chunk != null) {
                        int idx = level.getSectionIndexFromSectionY(y);
                        LevelChunkSection levelSection = idx >= 0 && idx < chunk.getSections().length ? chunk.getSections()[idx] : null;
                        hasBlocks = levelSection != null && !levelSection.hasOnlyAir();
                    }
                    boolean uncompiled = section.getCompiledSection() == CompiledSection.UNCOMPILED;
                    if (section.xOffset() >> 4 != x || section.yOffset() >> 4 != y || section.zOffset() >> 4 != z) {
                        out.append(String.format("%n  sec(%d,%d,%d) GRID MISMATCH holds (%d,%d,%d)", x, y, z,
                                section.xOffset() >> 4, section.yOffset() >> 4, section.zOffset() >> 4));
                        missing++;
                        continue;
                    }
                    if (!hasBlocks || !(uncompiled || section.isCompletelyEmpty()))
                        continue;
                    missing++;
                    out.append(String.format("%n  sec(%d,%d,%d) %s dirty=%b task=%b reached=%b ready=%b status=%d chunk=%b",
                            x, y, z, uncompiled ? "UNCOMPILED" : "EMPTY", section.isDirty(), section.hasBuildTask(),
                            section.getLastFrame() == renderer.getLastFrame(), ChunkStatusMap.INSTANCE.chunkRenderReady(x, z),
                            ChunkStatusMap.INSTANCE.status(x, z), chunk != null));
                }
            }
        }
        Initializer.LOGGER.info("[SECTION_DIAG] camera sec({},{},{}) {} sections with blocks but no geometry{}", cx, cy, cz, missing, out);
    }
}
