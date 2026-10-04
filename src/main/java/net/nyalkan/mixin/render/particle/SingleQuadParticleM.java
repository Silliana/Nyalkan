package net.nyalkan.mixin.render.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.nyalkan.interfaces.ExtendedParticle;
import net.nyalkan.interfaces.ExtendedVertexBuilder;
import net.nyalkan.render.chunk.RenderSection;
import net.nyalkan.render.chunk.WorldRenderer;
import net.nyalkan.vulkan.util.ColorUtil;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(SingleQuadParticle.class)
public abstract class SingleQuadParticleM extends Particle implements ExtendedParticle {

    @Unique private static final Quaternionf ROTATION = new Quaternionf();
    @Unique private static final BlockPos.MutableBlockPos LIGHT_POS = new BlockPos.MutableBlockPos();

    @Unique private static final ClassValue<Boolean> CUSTOM_QUAD = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return declaringClass(type, "renderRotatedQuad", VertexConsumer.class, Camera.class, Quaternionf.class, float.class) != SingleQuadParticle.class
                        || declaringClass(type, "renderRotatedQuad", VertexConsumer.class, Quaternionf.class, float.class, float.class, float.class, float.class) != SingleQuadParticle.class;
            } catch (NoSuchMethodException e) {
                return true;
            }
        }
    };

    @Unique private static final ClassValue<Boolean> GPU_ELIGIBLE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                return !CUSTOM_QUAD.get(type)
                        && declaringClass(type, "render", VertexConsumer.class, Camera.class, float.class) == SingleQuadParticle.class
                        && declaringClass(type, "getFacingCameraMode") == SingleQuadParticle.class;
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
    };

    @Shadow protected float quadSize;

    @Shadow protected abstract float getU0();
    @Shadow protected abstract float getU1();
    @Shadow protected abstract float getV0();
    @Shadow protected abstract float getV1();

    @Shadow public abstract float getQuadSize(float f);
    @Shadow public abstract SingleQuadParticle.FacingCameraMode getFacingCameraMode();
    @Shadow protected abstract void renderRotatedQuad(VertexConsumer buffer, Camera camera, Quaternionf quaternion, float partialTicks);

    protected SingleQuadParticleM(ClientLevel clientLevel, double d, double e, double f, double g, double h, double i) {
        super(clientLevel, d, e, f, g, h, i);
        this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.5F) * 2.0F;
    }

    @Overwrite
    public void render(VertexConsumer vertexConsumer, Camera camera, float partialTicks) {
        double lx = Mth.lerp(partialTicks, this.xo, this.x);
        double ly = Mth.lerp(partialTicks, this.yo, this.y);
        double lz = Mth.lerp(partialTicks, this.zo, this.z);

        if (cull(WorldRenderer.getInstance(), (float) lx, (float) ly, (float) lz))
            return;

        boolean renderThread = RenderSystem.isOnRenderThread();
        SingleQuadParticle.FacingCameraMode mode = this.getFacingCameraMode();
        if (mode != SingleQuadParticle.FacingCameraMode.LOOKAT_XYZ || !renderThread || CUSTOM_QUAD.get(this.getClass())) {
            Quaternionf rotation = renderThread ? ROTATION : new Quaternionf();
            mode.setRotation(rotation, camera, partialTicks);
            if (this.roll != 0.0F)
                rotation.rotateZ(Mth.lerp(partialTicks, this.oRoll, this.roll));
            this.renderRotatedQuad(vertexConsumer, camera, rotation, partialTicks);
            return;
        }

        Vec3 camPos = camera.getPosition();
        float x = (float) (lx - camPos.x());
        float y = (float) (ly - camPos.y());
        float z = (float) (lz - camPos.z());

        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        float rx = -left.x(), ry = -left.y(), rz = -left.z();
        float ux = up.x(), uy = up.y(), uz = up.z();

        if (this.roll != 0.0F) {
            float angle = Mth.lerp(partialTicks, this.oRoll, this.roll);
            float c = (float) Math.cos(angle);
            float s = (float) Math.sin(angle);
            float nrx = c * rx + s * ux, nry = c * ry + s * uy, nrz = c * rz + s * uz;
            float nux = c * ux - s * rx, nuy = c * uy - s * ry, nuz = c * uz - s * rz;
            rx = nrx; ry = nry; rz = nrz;
            ux = nux; uy = nuy; uz = nuz;
        }

        float size = this.getQuadSize(partialTicks);
        rx *= size; ry *= size; rz *= size;
        ux *= size; uy *= size; uz *= size;

        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);

        ExtendedVertexBuilder builder = ExtendedVertexBuilder.of(vertexConsumer);
        if (builder != null && builder.canFastParticle()) {
            int color = ColorUtil.RGBA.pack(this.rCol, this.gCol, this.bCol, this.alpha);
            builder.vertex(x + rx - ux, y + ry - uy, z + rz - uz, u1, v1, color, light);
            builder.vertex(x + rx + ux, y + ry + uy, z + rz + uz, u1, v0, color, light);
            builder.vertex(x - rx + ux, y - ry + uy, z - rz + uz, u0, v0, color, light);
            builder.vertex(x - rx - ux, y - ry - uy, z - rz - uz, u0, v1, color, light);
        } else {
            this.nyalkan$vertex(vertexConsumer, x + rx - ux, y + ry - uy, z + rz - uz, u1, v1, light);
            this.nyalkan$vertex(vertexConsumer, x + rx + ux, y + ry + uy, z + rz + uz, u1, v0, light);
            this.nyalkan$vertex(vertexConsumer, x - rx + ux, y - ry + uy, z - rz + uz, u0, v0, light);
            this.nyalkan$vertex(vertexConsumer, x - rx - ux, y - ry - uy, z - rz - uz, u0, v1, light);
        }
    }

    @Override
    public boolean nyalkan$gpuEligible() {
        return GPU_ELIGIBLE.get(this.getClass());
    }

    @Override
    public void nyalkan$writeInstance(long ptr, double originX, double originY, double originZ) {
        MemoryUtil.memPutFloat(ptr, (float) (this.xo - originX));
        MemoryUtil.memPutFloat(ptr + 4, (float) (this.yo - originY));
        MemoryUtil.memPutFloat(ptr + 8, (float) (this.zo - originZ));
        MemoryUtil.memPutFloat(ptr + 12, (float) (this.x - originX));
        MemoryUtil.memPutFloat(ptr + 16, (float) (this.y - originY));
        MemoryUtil.memPutFloat(ptr + 20, (float) (this.z - originZ));
        MemoryUtil.memPutFloat(ptr + 24, this.getQuadSize(0.0F));
        MemoryUtil.memPutFloat(ptr + 28, this.getQuadSize(1.0F));
        MemoryUtil.memPutFloat(ptr + 32, this.oRoll);
        MemoryUtil.memPutFloat(ptr + 36, this.roll);
        MemoryUtil.memPutFloat(ptr + 40, this.getU0());
        MemoryUtil.memPutFloat(ptr + 44, this.getV0());
        MemoryUtil.memPutFloat(ptr + 48, this.getU1());
        MemoryUtil.memPutFloat(ptr + 52, this.getV1());
        MemoryUtil.memPutInt(ptr + 56, ColorUtil.RGBA.pack(this.rCol, this.gCol, this.bCol, this.alpha));
        MemoryUtil.memPutInt(ptr + 60, this.getLightColor(0.5F));
    }

    @Unique
    private void nyalkan$vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, int light) {
        buffer.addVertex(x, y, z).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    protected int getLightColor(float f) {
        if (!RenderSystem.isOnRenderThread()) {
            BlockPos blockPos = BlockPos.containing(this.x, this.y, this.z);
            return this.level.hasChunkAt(blockPos) ? LevelRenderer.getLightColor(this.level, blockPos) : 0;
        }

        BlockPos.MutableBlockPos pos = LIGHT_POS.set(this.x, this.y, this.z);
        return this.level.hasChunkAt(pos) ? LevelRenderer.getLightColor(this.level, pos) : 0;
    }

    @Unique
    private static Class<?> declaringClass(Class<?> type, String name, Class<?>... params) throws NoSuchMethodException {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                c.getDeclaredMethod(name, params);
                return c;
            } catch (NoSuchMethodException ignored) {
            }
        }
        throw new NoSuchMethodException(name);
    }

    @Unique
    private boolean cull(WorldRenderer worldRenderer, float x, float y, float z) {
        RenderSection section = worldRenderer.getSectionGrid().getSectionAtBlockPos((int) x, (int) y, (int) z);
        return section != null && section.getLastFrame() != worldRenderer.getLastFrame();
    }

    @Override
    public ParticleRenderType getRenderType() {
        return null;
    }
}
