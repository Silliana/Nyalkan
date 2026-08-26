package net.vulkanmod.mixin.compat.veil;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.Program;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Mixin(value = ShaderInstance.class, priority = 100)
public class ShaderInstanceVeilRedirectM {

    @Shadow @Mutable private Program vertexProgram;
    @Shadow @Mutable private Program fragmentProgram;

    @Inject(method = "updateLocations", at = @At("TAIL"), cancellable = true)
    private void vulkanmod$skipVeilUniformScan(CallbackInfo ci) {
        ci.cancel();
    }

    // placeholder programs for veil
    @Inject(method = "<init>(Lnet/minecraft/server/packs/resources/ResourceProvider;Lnet/minecraft/resources/ResourceLocation;Lcom/mojang/blaze3d/vertex/VertexFormat;)V", at = @At("TAIL"))
    private void vulkanmod$fillVeilShaderRecompileFields(CallbackInfo ci) throws IOException {
        if (this.vertexProgram == null)
            this.vertexProgram = vulkanmod$placeholderProgram(Program.Type.VERTEX);
        if (this.fragmentProgram == null)
            this.fragmentProgram = vulkanmod$placeholderProgram(Program.Type.FRAGMENT);
    }

    private static Program vulkanmod$placeholderProgram(Program.Type type) throws IOException {
        InputStream dummySource = new ByteArrayInputStream("#version 150\nvoid main(){}".getBytes(StandardCharsets.UTF_8));
        return Program.compileShader(type, "vulkanmod$placeholder", dummySource, "vulkanmod", new GlslPreprocessor() {
            @Override
            public String applyImport(boolean useFullPath, String path) {
                return null;
            }
        });
    }
}
