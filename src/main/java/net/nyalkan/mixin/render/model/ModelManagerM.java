package net.nyalkan.mixin.render.model;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashMap;
import java.util.Map;

@Mixin(ModelManager.class)
public class ModelManagerM {
    @Unique private final Map<ModelResourceLocation, BakedModel> nyalkan$lookups = new LinkedHashMap<>(256, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<ModelResourceLocation, BakedModel> eldest) {
            return this.size() > 1024;
        }
    };

    @WrapMethod(method = "getModel(Lnet/minecraft/client/resources/model/ModelResourceLocation;)Lnet/minecraft/client/resources/model/BakedModel;")
    private BakedModel nyalkan$cachedModel(ModelResourceLocation location, Operation<BakedModel> original) {
        if (!RenderSystem.isOnRenderThread())
            return original.call(location);
        BakedModel model = this.nyalkan$lookups.get(location);
        if (model == null) {
            model = original.call(location);
            if (model != null)
                this.nyalkan$lookups.put(location, model);
        }
        return model;
    }

    @Inject(method = "apply", at = @At("TAIL"))
    private void nyalkan$clearLookups(CallbackInfo ci) {
        this.nyalkan$lookups.clear();
    }
}
