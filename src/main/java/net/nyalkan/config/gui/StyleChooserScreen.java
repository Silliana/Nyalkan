package net.nyalkan.config.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.nyalkan.Initializer;

public class StyleChooserScreen extends Screen {
    private static final int BUTTON_WIDTH = 150;
    private static final int GAP = 20;

    private final Screen parent;

    public StyleChooserScreen(Screen parent) {
        super(Component.translatable("nyalkan.options.settingsStyle.choose"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - BUTTON_WIDTH - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int y = this.height / 2 - 30;

        this.addRenderableWidget(Button.builder(Component.translatable("nyalkan.options.settingsStyle.modern"),
                b -> this.choose(SettingsStyle.MODERN)).bounds(left, y, BUTTON_WIDTH, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("nyalkan.options.settingsStyle.classic"),
                b -> this.choose(SettingsStyle.CLASSIC)).bounds(right, y, BUTTON_WIDTH, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);

        int y = this.height / 2 - 30;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, y - 40, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.translatable("nyalkan.options.settingsStyle.later"),
                this.width / 2, y - 25, 0xA0A0A0);

        int left = this.width / 2 - BUTTON_WIDTH - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        guiGraphics.drawWordWrap(this.font, Component.translatable("nyalkan.options.settingsStyle.modern.desc"),
                left, y + 28, BUTTON_WIDTH, 0xC0C0C0);
        guiGraphics.drawWordWrap(this.font, Component.translatable("nyalkan.options.settingsStyle.classic.desc"),
                right, y + 28, BUTTON_WIDTH, 0xC0C0C0);
    }

    @Override
    public void onClose() {
        this.choose(SettingsStyle.MODERN);
    }

    private void choose(int style) {
        Initializer.CONFIG.settingsStyle = style;
        Initializer.CONFIG.write();
        this.minecraft.setScreen(new VideoSettingsScreen(this.parent, this.minecraft, this.minecraft.options));
    }
}
