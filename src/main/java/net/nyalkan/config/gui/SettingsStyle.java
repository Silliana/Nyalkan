package net.nyalkan.config.gui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.nyalkan.Initializer;

public final class SettingsStyle {
    public static final int UNSET = 0;
    public static final int MODERN = 1;
    public static final int CLASSIC = 2;

    private SettingsStyle() {}

    // subclasses belong to mods
    public static Screen route(Screen screen) {
        if (screen == null || screen.getClass() != VideoSettingsScreen.class)
            return screen;

        Screen parent = ((VideoSettingsScreen) screen).lastScreen;
        return switch (Initializer.CONFIG.settingsStyle) {
            case CLASSIC -> screen;
            case MODERN -> modern(parent);
            default -> new StyleChooserScreen(parent);
        };
    }

    public static Screen modern(Screen parent) {
        return new VOptionScreen(Component.translatable("nyalkan.options.title"), parent);
    }
}
