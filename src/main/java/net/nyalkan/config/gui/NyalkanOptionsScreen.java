package net.nyalkan.config.gui;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.nyalkan.config.option.VanillaOptions;

import java.util.ArrayList;
import java.util.List;

public class NyalkanOptionsScreen extends OptionsSubScreen {
    private static final int MIN_WIDTH = 150;
    // room for longer values
    private static final int PADDING = 30;

    private WideOptionsList wideList;

    public NyalkanOptionsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("nyalkan.options.title"));
    }

    @Override
    protected void addContents() {
        List<AbstractWidget> widgets = new ArrayList<>();
        int widest = 0;
        for (OptionInstance<?> option : VanillaOptions.nyalkanPage()) {
            AbstractWidget widget = option.createButton(this.options);
            widgets.add(widget);
            widest = Math.max(widest, this.font.width(widget.getMessage()));
        }

        int column = Mth.clamp(widest + PADDING, MIN_WIDTH, Math.max(MIN_WIDTH, (this.width - 40) / 2));
        for (AbstractWidget widget : widgets)
            widget.setWidth(column);

        this.wideList = this.layout.addToContents(new WideOptionsList(this.minecraft, this.width, this.layout, column));
        this.wideList.addRows(widgets);
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
