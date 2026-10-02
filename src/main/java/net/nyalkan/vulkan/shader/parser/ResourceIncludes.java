package net.nyalkan.vulkan.shader.parser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

// vanilla moj_import lookup
public abstract class ResourceIncludes {

    public static Function<String, String> resolver(ResourceProvider resourceProvider) {
        return name -> {
            ResourceLocation location = ResourceLocation.tryParse(name);
            if (location == null)
                return null;

            Optional<Resource> resource = resourceProvider.getResource(location.withPrefix("shaders/include/"));
            if (resource.isEmpty())
                return null;

            try (InputStream inputStream = resource.get().open()) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        };
    }
}
