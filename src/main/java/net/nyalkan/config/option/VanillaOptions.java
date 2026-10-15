package net.nyalkan.config.option;

import com.mojang.serialization.Codec;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.nyalkan.Initializer;
import net.nyalkan.config.gui.OptionBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// our options as vanilla widgets
public final class VanillaOptions {
    private VanillaOptions() {}

    public static OptionInstance<?> windowMode() {
        return of(find(Options.getVideoOpts(), "nyalkan.options.windowMode"));
    }

    public static OptionInstance<?>[] nyalkanPage() {
        List<OptionInstance<?>> list = new ArrayList<>();
        for (OptionBlock[] page : List.of(Options.getOptimizationOpts(), Options.getOtherOpts()))
            for (OptionBlock block : page)
                for (Option<?> option : block.options())
                    list.add(of(option));
        return list.toArray(new OptionInstance<?>[0]);
    }

    // vanilla ones skip our config
    public static OptionInstance<?>[] replaceVanilla(OptionInstance<?>[] options, net.minecraft.client.Options vanilla) {
        OptionBlock[] graphics = Options.getGraphicsOpts();
        List<OptionInstance<?>> list = new ArrayList<>(Arrays.asList(options));
        list.remove(vanilla.fullscreen());
        list.replaceAll(o -> o == vanilla.graphicsMode() ? of(find(graphics, "options.graphics"))
                : o == vanilla.ambientOcclusion() ? of(find(graphics, "options.ao"))
                : o);
        return list.toArray(new OptionInstance<?>[0]);
    }

    private static Option<?> find(OptionBlock[] blocks, String key) {
        for (OptionBlock block : blocks)
            for (Option<?> option : block.options())
                if (key.equals(key(option)))
                    return option;
        throw new IllegalStateException("Missing option " + key);
    }

    private static String key(Option<?> option) {
        Component label = option.getLabel();
        return label.getContents() instanceof TranslatableContents contents ? contents.getKey() : label.getString();
    }

    @SuppressWarnings("unchecked")
    private static OptionInstance<?> of(Option<?> option) {
        OptionInstance.TooltipSupplier<Object> tooltip = option.getTooltip() != null
                ? OptionInstance.cachedConstantTooltip(option.getTooltip())
                : OptionInstance.noTooltip();

        if (option instanceof SwitchOption bool)
            return OptionInstance.createBoolean(key(option), tooltip::apply, bool.getNewValue(), value -> apply(bool, value));

        if (option instanceof RangeOption range) {
            int steps = (range.max - range.min) / range.step;
            return new OptionInstance<>(key(option), tooltip::apply,
                    (caption, value) -> net.minecraft.client.Options.genericValueLabel(caption, label(range, value)),
                    new OptionInstance.IntRange(0, steps).xmap(i -> range.min + i * range.step, v -> (v - range.min) / range.step),
                    Codec.INT, range.getNewValue(), value -> apply(range, value));
        }

        CyclingOption<Object> cycling = (CyclingOption<Object>) option;
        List<Object> values = List.of(cycling.getValues());
        Codec<Object> codec = Codec.INT.xmap(values::get, values::indexOf);
        // cycle buttons add caption
        return new OptionInstance<>(key(option), tooltip,
                (caption, value) -> label(cycling, value),
                new OptionInstance.Enum<>(values, codec), cycling.getNewValue(), value -> apply(cycling, value));
    }

    private static <T> Component label(Option<T> option, T value) {
        return option.translator != null ? option.translator.apply(value) : Component.literal(String.valueOf(value));
    }

    private static <T> void apply(Option<T> option, T value) {
        option.setNewValue(value);
        option.apply();
        Initializer.CONFIG.write();
    }
}
