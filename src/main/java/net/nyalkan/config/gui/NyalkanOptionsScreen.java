package net.nyalkan.config.gui;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.nyalkan.config.option.VanillaOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class NyalkanOptionsScreen extends OptionsSubScreen {
    private static final int MIN_WIDTH = 150;
    // room for longer values
    private static final int PADDING = 30;

    private final Supplier<OptionInstance<?>[]> page;
    private final boolean main;
    private WideOptionsList wideList;

    private NyalkanOptionsScreen(Screen lastScreen, Options options, Component title, Supplier<OptionInstance<?>[]> page, boolean main) {
        super(lastScreen, options, title);
        this.page = page;
        this.main = main;
    }

    public NyalkanOptionsScreen(Screen lastScreen, Options options) {
        this(lastScreen, options, Component.translatable("nyalkan.options.title"), VanillaOptions::nyalkanPage, true);
    }

    @Override
    protected void addContents() {
        List<AbstractWidget> widgets = new ArrayList<>();
        for (OptionInstance<?> option : this.page.get())
            widgets.add(option.createButton(this.options));
        if (this.main) {
            widgets.add(this.link("nyalkan.options.pages.details", VanillaOptions::detailsPage));
            widgets.add(this.link("nyalkan.options.pages.overlay", VanillaOptions::overlayPage));
            widgets.add(VanillaOptions.settingsStyle().createButton(this.options));
        }

        int widest = 0;
        for (AbstractWidget widget : widgets)
            widest = Math.max(widest, this.font.width(widget.getMessage()));

        int column = Mth.clamp(widest + PADDING, MIN_WIDTH, Math.max(MIN_WIDTH, (this.width - 40) / 2));
        for (AbstractWidget widget : widgets)
            widget.setWidth(column);

        this.wideList = this.layout.addToContents(new WideOptionsList(this.minecraft, this.width, this.layout, column));
        this.wideList.addRows(widgets);
    }

    private Button link(String key, Supplier<OptionInstance<?>[]> page) {
        Component title = Component.translatable(key);
        return Button.builder(Component.translatable("nyalkan.options.link", title),
                b -> this.minecraft.setScreen(new NyalkanOptionsScreen(this, this.options, title, page, false))).build();
    }

    @Override
    protected void addOptions() {
    }

    @Override
    protected void repositionElements() {
        super.repositionElements();
        if (this.wideList != null)
            this.wideList.updateSize(this.width, this.layout);
    }
}
