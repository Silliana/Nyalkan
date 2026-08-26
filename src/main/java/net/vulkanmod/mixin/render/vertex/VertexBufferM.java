package net.vulkanmod.mixin.render.vertex;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import net.vulkanmod.render.VBO;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VertexBuffer.class)
public class VertexBufferM {

    private VBO vbo;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void constructor(VertexBuffer.Usage usage, CallbackInfo ci) {
        vbo = new VBO();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenBuffers()I"))
    private int doNothing() {
        return 0;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_glGenVertexArrays()I"))
    private int doNothing2() {
        return 0;
    }

    @Overwrite
    public void bind() {}

    @Overwrite
    public static void unbind() {}

    @Overwrite
    public void upload(MeshData meshData) {
        vbo.upload(meshData);
    }

    @Overwrite
    public void uploadIndexBuffer(ByteBufferBuilder.Result result) {
        vbo.uploadIndexBuffer(result.byteBuffer());
    }

    @Overwrite
    public void drawWithShader(Matrix4f viewMatrix, Matrix4f projectionMatrix, ShaderInstance shader) {
        vbo.drawWithShader(viewMatrix, projectionMatrix, shader);
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void vulkanmod$draw(CallbackInfo ci) {
        vbo.draw();
        ci.cancel();
    }

    @Overwrite
    public void close() {
        vbo.close();
    }
}
