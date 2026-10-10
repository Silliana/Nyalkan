package net.nyalkan.mixin.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import net.nyalkan.render.chunk.WorldRenderer;
import net.nyalkan.render.occlusion.HiZ;
import net.nyalkan.render.model.BlockEntityReplay;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow @Final
    private RenderBuffers renderBuffers;

    @Shadow @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    @Shadow private int ticks;
    @Shadow @Final private Minecraft minecraft;
    @Shadow public abstract Frustum getFrustum();

    private WorldRenderer worldRenderer;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft minecraft, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers renderBuffers, CallbackInfo ci) {
        this.worldRenderer = WorldRenderer.init(this.renderBuffers);
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void setLevel(ClientLevel clientLevel, CallbackInfo ci) {
        this.worldRenderer.setLevel(clientLevel);
    }

    @Inject(method = "allChanged", at = @At("RETURN"))
    private void onAllChanged(CallbackInfo ci) {
        this.worldRenderer.allChanged();
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;render(Lnet/minecraft/world/level/block/entity/BlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void nyalkan$replayGlobal(BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity, float partialTick, PoseStack poseStack,
                                        MultiBufferSource source, Operation<Void> original) {
        if (blockEntity.getLevel() != this.minecraft.level)
            original.call(dispatcher, blockEntity, partialTick, poseStack, source);
        else
            BlockEntityReplay.render(dispatcher, blockEntity, partialTick, poseStack, source);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;checkPoseStack(Lcom/mojang/blaze3d/vertex/PoseStack;)V", ordinal = 1, shift = At.Shift.BEFORE))
    private void renderBlockEntities(DeltaTracker deltaTracker, boolean bl, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f matrix4f, Matrix4f matrix4f2, CallbackInfo ci) {
        Vec3 pos = camera.getPosition();
        PoseStack poseStack = new PoseStack();

        this.worldRenderer.renderBlockEntities(poseStack, pos.x(), pos.y(), pos.z(), this.destructionProgress, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    private void nyalkan$setupRender(Camera camera, Frustum frustum, boolean isCapturedFrustum, boolean spectator, CallbackInfo ci) {
        this.worldRenderer.setupRenderer(camera, frustum, isCapturedFrustum, spectator);
        ci.cancel();
    }

    @Overwrite
    public boolean isSectionCompiled(BlockPos blockPos) {
        return this.worldRenderer.isSectionCompiled(blockPos);
    }

    @Inject(method = "renderSectionLayer", at = @At("HEAD"), cancellable = true)
    private void nyalkan$renderSectionLayer(RenderType renderType, double camX, double camY, double camZ, Matrix4f modelView, Matrix4f projectionMatrix, CallbackInfo ci) {
        this.worldRenderer.renderSectionLayer(renderType, camX, camY, camZ, modelView, projectionMatrix);
        // opaque terrain done
        if (renderType == RenderType.cutout())
            HiZ.capture(modelView, projectionMatrix, camX, camY, camZ);
        // vanilla layer stage events
        ClientHooks.dispatchRenderStage(renderType, (LevelRenderer) (Object) this, modelView, projectionMatrix,
                this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.getFrustum());
        ci.cancel();
    }

    @Overwrite
    public void onChunkLoaded(ChunkPos chunkPos) {
    }

    @Overwrite
    private void setSectionDirty(int x, int y, int z, boolean flag) {
        this.worldRenderer.setSectionDirty(x, y, z, flag);
    }

    @Overwrite
    public String getSectionStatistics() {
        return this.worldRenderer.getChunkStatistics();
    }

    @Overwrite
    public boolean hasRenderedAllSections() {
        return !this.worldRenderer.graphNeedsUpdate() && this.worldRenderer.getTaskDispatcher().isIdle();
    }

    @Overwrite
    public int countRenderedSections() {
        return this.worldRenderer.getVisibleSectionsCount();
    }

    @Redirect(method = "renderWorldBorder", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;getDepthFar()F"))
    private float getRenderDistanceZFar(GameRenderer instance) {
        return instance.getRenderDistance() * 4F;
    }

}
