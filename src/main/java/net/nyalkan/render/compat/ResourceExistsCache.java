package net.nyalkan.render.compat;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// cleared on resource reload
public final class ResourceExistsCache {
    private static final Map<ResourceLocation, Boolean> CACHE = new ConcurrentHashMap<>();

    private ResourceExistsCache() {}

    public static boolean exists(ResourceLocation location, Predicate<ResourceLocation> lookup) {
        Boolean cached = CACHE.get(location);
        if (cached != null)
            return cached;
        boolean found = lookup.test(location);
        CACHE.put(location, found);
        return found;
    }

    public static void clear() {
        CACHE.clear();
    }
}
