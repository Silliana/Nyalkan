package net.nyalkan.config.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;

import java.util.List;

// two columns sized to labels
public class WideOptionsList extends ContainerObjectSelectionList<WideOptionsList.Row> {
    private static final int GAP = 10;

    private final int columnWidth;

    public WideOptionsList(Minecraft minecraft, int width, HeaderAndFooterLayout layout, int columnWidth) {
        super(minecraft, width, layout.getContentHeight(), layout.getHeaderHeight(), 25);
        this.centerListVertically = false;
        this.columnWidth = columnWidth;
    }

    public void addRows(List<AbstractWidget> widgets) {
        for (int i = 0; i < widgets.size(); i += 2)
            this.addEntry(new Row(i + 1 < widgets.size() ? widgets.subList(i, i + 2) : widgets.subList(i, i + 1)));
    }

    @Override
    public int getRowWidth() {
        return this.columnWidth * 2 + GAP;
    }

    public class Row extends ContainerObjectSelectionList.Entry<Row> {
        private final List<AbstractWidget> widgets;

        Row(List<AbstractWidget> widgets) {
            this.widgets = List.copyOf(widgets);
        }

        @Override
        public void render(GuiGraphics guiGraphics, int index, int top, int left, int width, int height,
                           int mouseX, int mouseY, boolean hovering, float partialTick) {
            int x = left;
            for (AbstractWidget widget : this.widgets) {
                widget.setPosition(x, top);
                widget.render(guiGraphics, mouseX, mouseY, partialTick);
                x += WideOptionsList.this.columnWidth + GAP;
            }
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.widgets;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.widgets;
        }
    }
}
