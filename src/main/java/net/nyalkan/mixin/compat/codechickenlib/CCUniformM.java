package net.nyalkan.mixin.compat.codechickenlib;

import com.mojang.blaze3d.shaders.Uniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

@Mixin(targets = "codechicken.lib.render.shader.CCUniform$UniformEntry", remap = false)
public abstract class CCUniformM {

    @Shadow protected Object cache;

    @Unique private ByteBuffer nyalkan$values;

    @Unique
    private ByteBuffer nyalkan$values() {
        if (this.nyalkan$values == null)
            this.nyalkan$values = ByteBuffer.allocateDirect(((Uniform) (Object) this).getCount() * 4).order(ByteOrder.nativeOrder());
        return this.nyalkan$values;
    }

    public FloatBuffer getFloatBuffer() {
        return nyalkan$values().asFloatBuffer();
    }

    public IntBuffer getIntBuffer() {
        return nyalkan$values().asIntBuffer();
    }

    @Inject(method = "set(Ljava/lang/Object;Z)V", at = @At("RETURN"))
    private void nyalkan$sync(Object values, boolean transpose, CallbackInfo ci) {
        ByteBuffer dst = nyalkan$values();
        int count = dst.capacity() / 4;

        switch (this.cache) {
            case float[] f -> {
                int side = transpose ? nyalkan$matrixSide(f.length) : 0;
                for (int i = 0; i < Math.min(count, f.length); i++)
                    dst.putFloat(i * 4, side > 0 ? f[(i % side) * side + i / side] : f[i]);
            }
            case int[] a -> {
                for (int i = 0; i < Math.min(count, a.length); i++)
                    dst.putInt(i * 4, a[i]);
            }
            case double[] d -> {
                for (int i = 0; i < Math.min(count, d.length); i++)
                    dst.putFloat(i * 4, (float) d[i]);
            }
            case null, default -> {}
        }
    }

    // skip raw GL upload
    @Inject(method = "upload", at = @At("HEAD"), cancellable = true)
    private void nyalkan$upload(CallbackInfo ci) {
        ci.cancel();
    }

    @Unique
    private static int nyalkan$matrixSide(int length) {
        return switch (length) {
            case 4 -> 2;
            case 9 -> 3;
            case 16 -> 4;
            default -> 0;
        };
    }
}
