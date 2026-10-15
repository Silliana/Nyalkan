package net.nyalkan.config.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchableOptionsList extends OptionsList {
    private static final int SMALL_WIDTH = 150;

    private final OptionsSubScreen screen;
    private final List<AbstractWidget> searchOnly = new ArrayList<>();
    private List<Entry> rows;

    public SearchableOptionsList(Minecraft minecraft, int width, OptionsSubScreen screen) {
        super(minecraft, width, screen);
        this.screen = screen;
    }

    public void addSearchOnly(AbstractWidget widget) {
        this.searchOnly.add(widget);
    }

    public void filter(String text) {
        if (this.rows == null)
            this.rows = new ArrayList<>(this.children());

        String query = text.trim().toLowerCase(Locale.ROOT);
        this.setScrollAmount(0.0);
        if (query.isEmpty()) {
            this.replaceEntries(this.rows);
            return;
        }

        List<AbstractWidget> small = new ArrayList<>();
        List<Entry> result = new ArrayList<>();
        for (Entry row : this.rows)
            for (GuiEventListener child : row.children())
                if (child instanceof AbstractWidget widget && matches(widget, query)) {
                    if (widget.getWidth() > SMALL_WIDTH)
                        result.add(Entry.big(List.of(widget), this.screen));
                    else
                        small.add(widget);
                }
        for (AbstractWidget widget : this.searchOnly)
            if (matches(widget, query))
                small.add(widget);

        for (int i = 0; i < small.size(); i += 2)
            result.add(Entry.small(small.get(i), i + 1 < small.size() ? small.get(i + 1) : null, this.screen));
        this.replaceEntries(result);
    }

    private static boolean matches(AbstractWidget widget, String query) {
        return widget.getMessage().getString().toLowerCase(Locale.ROOT).contains(query);
    }
}
