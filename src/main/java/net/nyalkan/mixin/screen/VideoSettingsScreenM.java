package net.nyalkan.mixin.screen;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.nyalkan.config.gui.SettingsStyle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// pairs with last option
//
// reimplements pairing loop
@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenM extends OptionsSubScreen {

    protected VideoSettingsScreenM(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Redirect(method = "addOptions", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/OptionsList;addSmall([Lnet/minecraft/client/OptionInstance;)V"))
    private void redirectAddSmallOptions(OptionsList list, OptionInstance<?>[] options) {
        Button nyalkanButton = Button.builder(Component.translatable("nyalkan.options.button"),
                        b -> this.minecraft.setScreen(SettingsStyle.modern(this)))
                .build();

        for (int i = 0; i < options.length; i += 2) {
            AbstractWidget left = options[i].createButton(this.options);
            AbstractWidget right = i < options.length - 1 ? options[i + 1].createButton(this.options) : nyalkanButton;
            list.addSmall(left, right);
        }
    }
}
