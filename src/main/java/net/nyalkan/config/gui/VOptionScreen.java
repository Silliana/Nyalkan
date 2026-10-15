package net.nyalkan.config.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.nyalkan.Initializer;
import net.nyalkan.config.gui.widget.VAbstractWidget;
import net.nyalkan.config.gui.widget.VButtonWidget;
import net.nyalkan.config.option.Option;
import net.nyalkan.config.option.OptionPage;
import net.nyalkan.config.option.Options;
import net.nyalkan.vulkan.util.ColorUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VOptionScreen extends Screen {
    public final static int RED = ColorUtil.ARGB.pack(0.3f, 0.0f, 0.0f, 0.8f);

    private final Screen parent;

    private final List<OptionPage> optionPages;

    private int currentListIdx = 0;

    private int tooltipX;
    private int tooltipY;
    private int tooltipWidth;

    private VButtonWidget doneButton;
    private VButtonWidget applyButton;
    private VButtonWidget undoButton;

    private final List<VButtonWidget> pageButtons = Lists.newArrayList();
    private final List<VButtonWidget> buttons = Lists.newArrayList();

    private EditBox searchBox;
    private String search = "";
    private VOptionList searchList;
    private VOptionList shownList;
    private int listX, listY, listWidth, listHeight, itemHeight;

    public VOptionScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;

        this.optionPages = new ArrayList<>();
    }

    private void addPages() {
        this.optionPages.clear();

        OptionPage page = new OptionPage(
                Component.translatable("nyalkan.options.pages.video").getString(),
                Options.getVideoOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("nyalkan.options.pages.graphics").getString(),
                Options.getGraphicsOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("nyalkan.options.pages.optimizations").getString(),
                Options.getOptimizationOpts()
        );
        this.optionPages.add(page);

        page = new OptionPage(
                Component.translatable("nyalkan.options.pages.other").getString(),
                Options.getOtherOpts()
        );
        this.optionPages.add(page);
    }

    @Override
    protected void init() {
        this.addPages();

        int top = 40;
        int bottom = 60;
        int itemHeight = 20;

        int leftMargin = 100;
//        int listWidth = (int) (this.width * 0.65f);
        int listWidth = Math.min((int) (this.width * 0.65f), 420);
        int listHeight = this.height - top - bottom;

        this.listX = leftMargin;
        this.listY = top;
        this.listWidth = listWidth;
        this.listHeight = listHeight;
        this.itemHeight = itemHeight;
        this.buildLists(leftMargin, top, listWidth, listHeight, itemHeight);
        this.searchList = this.buildSearchList(this.search);

        int x = leftMargin + listWidth + 10;
//        int width = Math.min(this.width - this.tooltipX - 10, 200);
        int width = this.width - x - 10;
        int y = 50;

        if (width < 200) {
            x = 100;
            width = listWidth;
            y = this.height - bottom + 10;
        }

        this.tooltipX = x;
        this.tooltipY = y;
        this.tooltipWidth = width;

        buildPage();

        this.applyButton.active = false;
        this.undoButton.active = false;
    }

    private void buildLists(int left, int top, int listWidth, int listHeight, int itemHeight) {
        for (OptionPage page : this.optionPages) {
            page.createList(left, top, listWidth, listHeight, itemHeight);
        }
    }

    private void addPageButtons(int x0, int y0, int width, int height, boolean verticalLayout) {
        int x = x0;
        int y = y0;
        for (int i = 0; i < this.optionPages.size(); ++i) {
            var page = this.optionPages.get(i);
            final int finalIdx = i;
            VButtonWidget widget = new VButtonWidget(x, y, width, height, Component.nullToEmpty(page.name), button -> this.setOptionList(finalIdx));
            this.buttons.add(widget);
            this.pageButtons.add(widget);
            this.addWidget(widget);

            if (verticalLayout)
                y += height + 1;
            else
                x += width + 1;
        }

        if (this.searchList == null)
            this.pageButtons.get(this.currentListIdx).setSelected(true);
    }

    private void buildPage() {
        this.buttons.clear();
        this.pageButtons.clear();
        this.clearWidgets();

        this.searchBox = new EditBox(this.font, this.listX, 12, this.listWidth, 18, Component.translatable("nyalkan.options.search"));
        this.searchBox.setHint(Component.translatable("nyalkan.options.search"));
        this.searchBox.setValue(this.search);
        this.searchBox.setResponder(this::onSearch);
        this.addWidget(this.searchBox);

        this.addPageButtons(10, 40, 80, 22, true);

        this.shownList = this.currentList();
        this.addWidget(this.shownList);

        this.addButtons();
    }

    private void addButtons() {
        int rightMargin = 20;
        int buttonHeight = 20;
        int padding = 10;
        int buttonMargin = 5;
        int buttonWidth = minecraft.font.width(CommonComponents.GUI_DONE) + 2 * padding;
        int x0 = (this.width - buttonWidth - rightMargin);
        int y0 = this.height - buttonHeight - 7;

        this.doneButton = new VButtonWidget(
                x0, y0,
                buttonWidth, buttonHeight,
                CommonComponents.GUI_DONE,
                button -> {
                    this.applyOptions();
                    this.minecraft.setScreen(this.parent);
                }
        );

        buttonWidth = minecraft.font.width(Component.translatable("nyalkan.options.buttons.apply")) + 2 * padding;
        x0 -= (buttonWidth + buttonMargin);
        this.applyButton = new VButtonWidget(
                x0, y0,
                buttonWidth, buttonHeight,
                Component.translatable("nyalkan.options.buttons.apply"),
                button -> this.applyOptions()
        );

        buttonWidth = minecraft.font.width(Component.translatable("nyalkan.options.buttons.undo")) + 2 * padding;
        x0 -= (buttonWidth + buttonMargin);
        this.undoButton = new VButtonWidget(
                x0, y0,
                buttonWidth, buttonHeight,
                Component.translatable("nyalkan.options.buttons.undo"),
                button -> this.rebuildWidgets()
        );

        this.buttons.add(this.undoButton);
        this.buttons.add(this.applyButton);
        this.buttons.add(this.doneButton);

        this.addWidget(this.undoButton);
        this.addWidget(this.applyButton);
        this.addWidget(this.doneButton);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GuiEventListener element : this.children()) {
            if (element.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(element);
                if (button == 0) {
                    this.setDragging(true);
                }

                this.updateState();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.setDragging(false);
        this.updateState();
        return this.getChildAt(mouseX, mouseY)
                .filter(guiEventListener -> guiEventListener.mouseReleased(mouseX, mouseY, button))
                .isPresent();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        if (this.minecraft.level == null) {
            this.renderPanorama(guiGraphics, f);
        }

        this.renderBlurredBackground(f);
        this.renderMenuBackground(guiGraphics);

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, 0, 0, delta);

        GuiRenderer.guiGraphics = guiGraphics;
        GuiRenderer.setPoseStack(guiGraphics.pose());

        RenderSystem.enableBlend();

        this.searchBox.render(guiGraphics, mouseX, mouseY, delta);

        VOptionList currentList = this.currentList();
        currentList.updateState(mouseX, mouseY);
        currentList.renderWidget(mouseX, mouseY);
        renderButtons(mouseX, mouseY);

        if (currentList.getTotalLength() == 0)
            guiGraphics.drawCenteredString(this.font, Component.translatable("nyalkan.options.search.none"),
                    this.listX + this.listWidth / 2, this.listY + 10, 0xA0A0A0);

        List<FormattedCharSequence> list = getHoveredButtonTooltip(currentList, mouseX, mouseY);
        if (list != null) {
            this.renderTooltip(list, this.tooltipX, this.tooltipY);
        }
    }

    public void renderButtons(int mouseX, int mouseY) {
        for (VButtonWidget button : buttons) {
            button.render(mouseX, mouseY);
        }
    }

    private void renderTooltip(List<FormattedCharSequence> list, int x, int y) {
        int padding = 3;
        int width = GuiRenderer.getMaxTextWidth(this.font, list);
        int height = list.size() * 10;
        float intensity = 0.05f;
        int color = ColorUtil.ARGB.pack(intensity, intensity, intensity, 0.6f);
        GuiRenderer.fill(x - padding, y - padding, x + width + padding, y + height + padding, color);

//        intensity = 0.4f;
//        color = ColorUtil.ARGB.pack(intensity, intensity, intensity, 0.9f);
        color = RED;
        GuiRenderer.renderBorder(x - padding, y - padding, x + width + padding, y + height + padding, 1, color);

        int yOffset = 0;
        for (var text : list) {
            GuiRenderer.drawString(this.font, text, x, y + yOffset, 0xffffffff);
            yOffset += 10;
        }
    }

    private List<FormattedCharSequence> getHoveredButtonTooltip(VOptionList buttonList, int mouseX, int mouseY) {
        VAbstractWidget widget = buttonList.getHoveredWidget(mouseX, mouseY);
        if (widget != null) {
            var tooltip = widget.getTooltip();
            if (tooltip == null)
                return null;

            return this.font.split(tooltip, this.tooltipWidth);
        }
        return null;
    }

    private void updateState() {
        boolean modified = false;
        for (var page : this.optionPages) {
            modified |= page.optionChanged();
        }

        this.applyButton.active = modified;
        this.undoButton.active = modified;
    }

    private VOptionList currentList() {
        return this.searchList != null ? this.searchList : this.optionPages.get(this.currentListIdx).getOptionList();
    }

    private VOptionList buildSearchList(String text) {
        String query = text.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty())
            return null;

        List<Option<?>> matches = new ArrayList<>();
        for (OptionPage page : this.optionPages)
            for (OptionBlock block : page.getOptionBlocks())
                for (Option<?> option : block.options())
                    if (option.getLabel().getString().toLowerCase(Locale.ROOT).contains(query))
                        matches.add(option);

        VOptionList list = new VOptionList(this.listX, this.listY, this.listWidth, this.listHeight, this.itemHeight);
        list.addAll(matches.toArray(new Option<?>[0]));
        return list;
    }

    private void onSearch(String text) {
        this.search = text;
        this.searchList = this.buildSearchList(text);

        this.removeWidget(this.shownList);
        this.shownList = this.currentList();
        this.addWidget(this.shownList);

        for (int i = 0; i < this.pageButtons.size(); i++)
            this.pageButtons.get(i).setSelected(this.searchList == null && i == this.currentListIdx);
    }

    private void setOptionList(int i) {
        this.currentListIdx = i;
        this.search = "";
        this.searchList = null;

        this.buildPage();

        this.pageButtons.get(i).setSelected(true);
    }

    private void applyOptions() {
        List<OptionPage> pages = List.copyOf(this.optionPages);
        for (var page : pages) {
            page.applyOptionChanges();
        }

        Initializer.CONFIG.write();
        this.minecraft.options.save();
        this.updateState();
    }
}
