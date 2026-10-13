package net.nyalkan.render.sky;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.phys.Vec3;
import net.nyalkan.Initializer;
import net.nyalkan.render.PipelineManager;
import net.nyalkan.render.VBO;
import net.nyalkan.vulkan.RenderStateSnapshot;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;

public class CloudRenderer {
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");

    private static final int DIR_NEG_Y_BIT = 1 << 0;
    private static final int DIR_POS_Y_BIT = 1 << 1;
    private static final int DIR_NEG_X_BIT = 1 << 2;
    private static final int DIR_POS_X_BIT = 1 << 3;
    private static final int DIR_NEG_Z_BIT = 1 << 4;
    private static final int DIR_POS_Z_BIT = 1 << 5;

    private static final byte Y_ABOVE_CLOUDS = 0;
    private static final byte Y_BELOW_CLOUDS = 1;
    private static final byte Y_INSIDE_CLOUDS = 2;

    private static final int CELL_WIDTH = 12;
    private static final int CELL_HEIGHT = 4;
    private static final int RANGE = 32;

    private final RenderStateSnapshot savedState = new RenderStateSnapshot();

    private int[] pixels;
    private byte[] renderFaces;
    private int width;

    private int prevCellX;
    private int prevCellZ;
    private byte prevYState = -1;
    private CloudStatus prevCloudsType;

    private VBO cloudBuffer;
    private boolean empty;

    public CloudRenderer() {
        this.loadTexture();
    }

    public void loadTexture() {
        this.resetBuffer();
        try (InputStream stream = Minecraft.getInstance().getResourceManager().getResourceOrThrow(TEXTURE_LOCATION).open();
             NativeImage image = NativeImage.read(stream)) {
            if (image.getWidth() != image.getHeight())
                throw new IOException("Cloud texture must be square");
            this.width = image.getWidth();
            this.pixels = image.getPixelsRGBA();
        } catch (IOException | RuntimeException e) {
            Initializer.LOGGER.error("Failed to load cloud texture", e);
            this.width = 0;
            this.pixels = null;
            return;
        }

        // ABGR to ARGB
        for (int i = 0; i < this.pixels.length; i++)
            this.pixels[i] = FastColor.ABGR32.fromArgb32(this.pixels[i]);
        this.renderFaces = this.computeRenderFaces();
    }

    public void resetBuffer() {
        if (this.cloudBuffer != null) {
            this.cloudBuffer.close();
            this.cloudBuffer = null;
        }
        this.empty = false;
    }

    public void renderClouds(ClientLevel level, float cloudHeight, PoseStack poseStack, Matrix4f modelView, Matrix4f projection,
                             int ticks, float partialTicks, double camX, double camY, double camZ) {
        if (this.pixels == null)
            return;

        CloudStatus cloudsType = Minecraft.getInstance().options.getCloudsType();
        double centerX = camX + (ticks + partialTicks) * 0.03F;
        double centerZ = camZ + 0.33F * CELL_WIDTH;
        float centerY = (float) (cloudHeight - camY + 0.33F);

        int cellX = (int) Math.floor(centerX / CELL_WIDTH);
        int cellZ = (int) Math.floor(centerZ / CELL_WIDTH);

        byte yState = centerY < -CELL_HEIGHT ? Y_ABOVE_CLOUDS : centerY > 0.0f ? Y_BELOW_CLOUDS : Y_INSIDE_CLOUDS;

        if (cellX != this.prevCellX || cellZ != this.prevCellZ || cloudsType != this.prevCloudsType
                || yState != this.prevYState || this.cloudBuffer == null && !this.empty) {
            this.prevCellX = cellX;
            this.prevCellZ = cellZ;
            this.prevCloudsType = cloudsType;
            this.prevYState = yState;
            this.rebuild(cellX, cellZ, centerY);
        }

        if (this.cloudBuffer == null)
            return;

        FogRenderer.levelFogColor();

        poseStack.pushPose();
        poseStack.mulPose(modelView);
        poseStack.translate(-(float) (centerX - cellX * CELL_WIDTH), centerY, -(float) (centerZ - cellZ * CELL_WIDTH));

        Vec3 cloudColor = level.getCloudColor(partialTicks);
        boolean fast = cloudsType == CloudStatus.FAST;

        this.savedState.capture();
        try {
            RenderSystem.setShaderColor((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z, 0.8f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(true);
            if (yState == Y_INSIDE_CLOUDS || fast && centerY <= 0.0f)
                RenderSystem.disableCull();
            else
                RenderSystem.enableCull();

            Matrix4f pose = poseStack.last().pose();
            if (!fast) {
                RenderSystem.colorMask(false, false, false, false);
                this.cloudBuffer.drawWithShader(pose, projection, PipelineManager.getCloudsPipeline());
                RenderSystem.colorMask(true, true, true, true);
            }
            this.cloudBuffer.drawWithShader(pose, projection, PipelineManager.getCloudsPipeline());
        } finally {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            this.savedState.restore();
            poseStack.popPose();
        }
    }

    private void rebuild(int cellX, int cellZ, float centerY) {
        this.resetBuffer();
        MeshData mesh = this.buildClouds(cellX, cellZ, centerY);
        if (mesh == null) {
            this.empty = true;
            return;
        }
        this.cloudBuffer = new VBO();
        this.cloudBuffer.upload(mesh);
    }

    private MeshData buildClouds(int centerCellX, int centerCellZ, float cloudY) {
        BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean fancy = this.prevCloudsType == CloudStatus.FANCY;
        boolean inside = this.prevYState == Y_INSIDE_CLOUDS;
        final float w = CELL_WIDTH, h = CELL_HEIGHT;

        for (int dx = -RANGE; dx < RANGE; ++dx) {
            for (int dz = -RANGE; dz < RANGE; ++dz) {
                int idx = this.wrappedIdx(centerCellX + dx, centerCellZ + dz);
                byte faces = this.renderFaces[idx];
                if (faces == 0)
                    continue;

                int color = this.pixels[idx];
                float x = dx * w;
                float z = dz * w;

                if (!fancy) {
                    int c = shade(color, 1.0f);
                    quad(builder, c, x, 0, z + w, x, 0, z, x + w, 0, z, x + w, 0, z + w);
                    continue;
                }

                if ((faces & DIR_POS_Y_BIT) != 0 && cloudY <= 0.0f) {
                    int c = shade(color, 1.0f);
                    quad(builder, c, x + w, h, z + w, x + w, h, z, x, h, z, x, h, z + w);
                }
                if ((faces & DIR_NEG_Y_BIT) != 0 && cloudY >= -h) {
                    int c = shade(color, 0.7f);
                    quad(builder, c, x, 0, z + w, x, 0, z, x + w, 0, z, x + w, 0, z + w);
                }
                if ((faces & DIR_POS_X_BIT) != 0 && (x < 1.0f || inside)) {
                    int c = shade(color, 0.9f);
                    quad(builder, c, x + w, h, z + w, x + w, 0, z + w, x + w, 0, z, x + w, h, z);
                }
                if ((faces & DIR_NEG_X_BIT) != 0 && (x > -1.0f || inside)) {
                    int c = shade(color, 0.9f);
                    quad(builder, c, x, h, z, x, 0, z, x, 0, z + w, x, h, z + w);
                }
                if ((faces & DIR_POS_Z_BIT) != 0 && (z < 1.0f || inside)) {
                    int c = shade(color, 0.8f);
                    quad(builder, c, x, h, z + w, x, 0, z + w, x + w, 0, z + w, x + w, h, z + w);
                }
                if ((faces & DIR_NEG_Z_BIT) != 0 && (z > -1.0f || inside)) {
                    int c = shade(color, 0.8f);
                    quad(builder, c, x + w, h, z, x + w, 0, z, x, 0, z, x, h, z);
                }
            }
        }

        return builder.build();
    }

    private static void quad(BufferBuilder builder, int color, float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        builder.addVertex(x0, y0, z0).setColor(color);
        builder.addVertex(x1, y1, z1).setColor(color);
        builder.addVertex(x2, y2, z2).setColor(color);
        builder.addVertex(x3, y3, z3).setColor(color);
    }

    private static int shade(int color, float brightness) {
        return FastColor.ARGB32.color(FastColor.ARGB32.alpha(color),
                (int) (FastColor.ARGB32.red(color) * brightness),
                (int) (FastColor.ARGB32.green(color) * brightness),
                (int) (FastColor.ARGB32.blue(color) * brightness));
    }

    private byte[] computeRenderFaces() {
        byte[] faces = new byte[this.pixels.length];
        for (int z = 0; z < this.width; z++) {
            for (int x = 0; x < this.width; x++) {
                int pixel = this.pixels[z * this.width + x];
                if ((pixel >>> 24) <= 1)
                    continue;

                byte f = DIR_NEG_Y_BIT | DIR_POS_Y_BIT;
                if (pixel != this.pixels[this.wrappedIdx(x - 1, z)])
                    f |= DIR_NEG_X_BIT;
                if (pixel != this.pixels[this.wrappedIdx(x + 1, z)])
                    f |= DIR_POS_X_BIT;
                if (pixel != this.pixels[this.wrappedIdx(x, z - 1)])
                    f |= DIR_NEG_Z_BIT;
                if (pixel != this.pixels[this.wrappedIdx(x, z + 1)])
                    f |= DIR_POS_Z_BIT;
                faces[z * this.width + x] = f;
            }
        }
        return faces;
    }

    private int wrappedIdx(int x, int z) {
        return Math.floorMod(z, this.width) * this.width + Math.floorMod(x, this.width);
    }
}
