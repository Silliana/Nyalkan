package net.nyalkan.render.compat;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.nyalkan.Initializer;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class CatJammiesCache {
    private static final String DETECTOR = "com.buuz135.catjammies.IJammyDetector";

    private static boolean disabled = !ModList.get().isLoaded("catjammies");
    private static Class<?> detectorClass;
    private static List<Object> detectors;
    private static int wrappedSize = -1;

    private CatJammiesCache() {}

    @SuppressWarnings("unchecked")
    public static void ensureWrapped() {
        if (disabled)
            return;

        try {
            if (detectors == null) {
                detectorClass = Class.forName(DETECTOR, false, CatJammiesCache.class.getClassLoader());
                detectors = (List<Object>) detectorClass.getField("DETECTORS").get(null);
            }

            if (detectors.size() == wrappedSize)
                return;

            for (int i = 0; i < detectors.size(); i++) {
                Object detector = detectors.get(i);
                if (!Proxy.isProxyClass(detector.getClass()))
                    detectors.set(i, Proxy.newProxyInstance(detectorClass.getClassLoader(), new Class<?>[]{detectorClass}, new Cached(detector)));
            }
            wrappedSize = detectors.size();
        } catch (ReflectiveOperationException | RuntimeException e) {
            disabled = true;
            Initializer.LOGGER.warn("Cat Jammies cache disabled", e);
        }
    }

    private static final class Cached implements InvocationHandler {
        private final Object original;
        private final Map<Entity, long[]> results = new WeakHashMap<>();

        Cached(Object original) {
            this.original = original;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!method.getName().equals("detect") || args == null || args.length != 2
                    || !(args[0] instanceof Level level) || !(args[1] instanceof Entity entity))
                return call(method, args);

            long tick = level.getGameTime();
            long[] cached = this.results.get(entity);
            if (cached != null && cached[0] == tick)
                return (int) cached[1];

            int value = (Integer) call(method, args);
            if (cached == null)
                this.results.put(entity, new long[]{tick, value});
            else {
                cached[0] = tick;
                cached[1] = value;
            }
            return value;
        }

        private Object call(Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(this.original, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
