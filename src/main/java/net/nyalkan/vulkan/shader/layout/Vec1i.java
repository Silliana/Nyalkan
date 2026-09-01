package net.nyalkan.vulkan.shader.layout;

import net.nyalkan.vulkan.shader.Uniforms;
import net.nyalkan.vulkan.util.MappedBuffer;
import org.apache.commons.lang3.Validate;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Supplier;

public class Vec1i extends Uniform {
    private Supplier<Integer> intSupplier;

    public Vec1i(Info info) {
        super(info);
    }

    void setSupplier() {
        this.intSupplier = Uniforms.vec1i_uniformMap.get(this.info.name);
    }

    @Override
    public void setSupplier(Supplier<MappedBuffer> supplier) {
        this.intSupplier = () -> supplier.get().getInt(0);
    }

    void update(long ptr) {
        if (this.intSupplier == null) {
            return;
        }

        int i = this.intSupplier.get();
        MemoryUtil.memPutInt(ptr + this.offset, i);
    }
}
